package com.receipiti.be.domain.expenditure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class CardNotificationAnalysisService {

    private static final String MODEL = "gemini-2.5-flash";
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp",
            "image/heic",
            "image/heif"
    );
    private static final String PROMPT = """
            이 이미지는 카드사의 결제 알림 화면일 수 있습니다.
            이미지에 실제로 표시된 내용만 읽어 소비 정보를 추출하세요.
            보이지 않는 카드사, 가맹점, 날짜를 추측하지 마세요.

            규칙:
            - 결제 승인 또는 결제 취소 알림이 아니면 paymentNotification은 false입니다.
            - cardCompany는 카드사 이름, storeName은 결제 가맹점명입니다.
            - amount는 쉼표와 통화 기호를 제외한 정수 금액입니다.
            - paymentDateTime은 화면에서 확인 가능한 형태 그대로 반환합니다.
              날짜와 시간이 모두 명확하면 yyyy-MM-dd'T'HH:mm:ss 형식을 사용합니다.
            - currency는 KRW, USD 같은 ISO 통화 코드입니다. 원화 표시는 KRW입니다.
            - approvalStatus는 APPROVED, CANCELED, UNKNOWN 중 하나입니다.
            - confidence는 전체 추출 결과에 대한 0 이상 1 이하의 신뢰도입니다.
            - 확인할 수 없는 문자열은 빈 문자열, 금액은 0으로 반환하세요.
            """;

    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    public CardNotificationAnalysisService(
            @Qualifier("geminiRestClient") RestClient geminiRestClient,
            ObjectMapper objectMapper
    ) {
        this.geminiRestClient = geminiRestClient;
        this.objectMapper = objectMapper;
    }

    public CardNotificationAnalysisResponse analyze(MultipartFile file) {
        validateImage(file);

        try {
            Map<String, Object> request = createRequest(file);
            JsonNode response = geminiRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(MODEL))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            CardNotificationAnalysisResponse result = parseResponse(response);
            validateResult(result);
            return result;
        } catch (GeneralException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            log.warn("Gemini 카드 알림 분석 응답 오류: status={}", exception.getStatusCode());
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_ANALYSIS_FAILED);
        } catch (RestClientException | IOException exception) {
            log.warn("Gemini 카드 알림 분석 실패", exception);
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_ANALYSIS_FAILED);
        }
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new GeneralException(GeneralErrorCode.IMAGE_REQUIRED);
        }
        if (!SUPPORTED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new GeneralException(GeneralErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }

    private Map<String, Object> createRequest(MultipartFile file) throws IOException {
        Map<String, Object> textPart = Map.of("text", PROMPT);
        Map<String, Object> imagePart = Map.of("inline_data", Map.of(
                "mime_type", file.getContentType(),
                "data", Base64.getEncoder().encodeToString(file.getBytes())
        ));

        return Map.of(
                "contents", List.of(Map.of("parts", List.of(textPart, imagePart))),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", MediaType.APPLICATION_JSON_VALUE,
                        "responseSchema", responseSchema()
                )
        );
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                        "paymentNotification", Map.of("type", "BOOLEAN"),
                        "cardCompany", Map.of("type", "STRING"),
                        "storeName", Map.of("type", "STRING"),
                        "amount", Map.of("type", "INTEGER"),
                        "paymentDateTime", Map.of("type", "STRING"),
                        "currency", Map.of("type", "STRING"),
                        "approvalStatus", Map.of(
                                "type", "STRING",
                                "enum", List.of("APPROVED", "CANCELED", "UNKNOWN")
                        ),
                        "confidence", Map.of("type", "NUMBER")
                ),
                "required", List.of(
                        "paymentNotification", "cardCompany", "storeName", "amount",
                        "paymentDateTime", "currency", "approvalStatus", "confidence"
                )
        );
    }

    private CardNotificationAnalysisResponse parseResponse(JsonNode response) throws IOException {
        JsonNode text = response == null
                ? null
                : response.at("/candidates/0/content/parts/0/text");
        if (text == null || !text.isTextual() || text.asText().isBlank()) {
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_ANALYSIS_FAILED);
        }
        return objectMapper.readValue(text.asText(), CardNotificationAnalysisResponse.class);
    }

    private void validateResult(CardNotificationAnalysisResponse result) {
        if (!result.paymentNotification()
                || result.storeName() == null
                || result.storeName().isBlank()
                || result.amount() == null
                || result.amount() <= 0) {
            throw new GeneralException(GeneralErrorCode.CARD_NOTIFICATION_INFORMATION_INSUFFICIENT);
        }
    }
}
