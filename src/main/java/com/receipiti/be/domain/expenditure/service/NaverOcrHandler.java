package com.receipiti.be.domain.expenditure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NaverOcrHandler {

    private static final Set<String> RECEIPT_HEADER_WORDS = Set.of(
            "영수증", "카드판매", "카드판매영수증", "고객용", "매장명", "가맹점명"
    );

    private final RestTemplate restTemplate = new  RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${naver.ocr.url}")
    private String naverOcrUrl;

    @Value("${naver.ocr.secret}")
    private String naverOcrSecret;

    public OcrResponse executeOcr(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-OCR-SECRET", naverOcrSecret);

            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("version", "V2");
            messageMap.put("requestId", UUID.randomUUID().toString());
            messageMap.put("timestamp", System.currentTimeMillis());

            Map<String, String> imageMap = new HashMap<>();
            imageMap.put("format", getFileExtension(file.getOriginalFilename()));
            imageMap.put("name", "receipt");
            messageMap.put("images", List.of(imageMap));

            String jsonMessage = objectMapper.writeValueAsString(messageMap);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("message", jsonMessage);

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(naverOcrUrl, requestEntity, String.class);

            return parseOcrResponse(responseEntity.getBody());

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("Naver OCR API 호출 실패: ", e);
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    OcrResponse parseOcrResponse(String jsonResponseBody) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponseBody);
            JsonNode images = root.path("images");
            if (!images.isArray() || images.isEmpty()) {
                throw new GeneralException(GeneralErrorCode.RECEIPT_INFORMATION_INSUFFICIENT);
            }
            JsonNode fields = images.get(0).path("fields");

            StringBuilder fullTextBuilder = new StringBuilder();
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    fullTextBuilder.append(field.path("inferText").asText("")).append(" ");
                }
            }
            String fullText = fullTextBuilder.toString().trim();

            String storeName = "알 수 없는 상호명";
            Long totalPrice = 0L;
            LocalDateTime paymentDate = null;
            double confidenceSum = 0.0;
            int confidenceCount = 0;

            // 상호명 추출
            if (fullText.contains("[매장명]") || fullText.contains("[가맹점명]")) {
                Pattern storePattern = Pattern.compile(
                        "(?:\\[매장명\\]|\\[가맹점명\\])\\s*:?\\s*(.+?)(?=\\s*(?:\\[|사업자|대표|주소|전화|일시|승인|$))"
                );
                Matcher storeMatcher = storePattern.matcher(fullText);
                if (storeMatcher.find()) {
                    String candidate = normalizeStoreName(storeMatcher.group(1));
                    if (isMeaningfulStoreName(candidate)) {
                        storeName = candidate;
                    }
                }
            } else if (fields.size() > 0) {
                for (JsonNode field : fields) {
                    String candidate = normalizeStoreName(field.path("inferText").asText(""));
                    if (isMeaningfulStoreName(candidate)) {
                        storeName = candidate;
                        break;
                    }
                }
            }

            // 날짜 추출
            Pattern datePattern = Pattern.compile("(\\d{4})[-./](\\d{2})[-./](\\d{2})");
            Matcher dateMatcher = datePattern.matcher(fullText);
            if (dateMatcher.find()) {
                int year = Integer.parseInt(dateMatcher.group(1));
                int month = Integer.parseInt(dateMatcher.group(2));
                int day = Integer.parseInt(dateMatcher.group(3));

                int hour = 12, minute = 0, second = 0;
                Pattern timePattern = Pattern.compile("(\\d{2}):(\\d{2}):(\\d{2})");
                Matcher timeMatcher = timePattern.matcher(fullText);
                if (timeMatcher.find()) {
                    hour = Integer.parseInt(timeMatcher.group(1));
                    minute = Integer.parseInt(timeMatcher.group(2));
                    second = Integer.parseInt(timeMatcher.group(3));
                }
                try {
                    paymentDate = LocalDateTime.of(year, month, day, hour, minute, second);
                } catch (DateTimeException ignored) {
                    paymentDate = null;
                }
            }

            // 금액 추출
            Pattern pricePattern = Pattern.compile("(?:합계|결제|받을|신용\\s*카드)\\s*(?:금액)?\\s*:?\\s*([0-9,]{4,10})");
            Matcher priceMatcher = pricePattern.matcher(fullText);

            if (priceMatcher.find()) {
                String priceStr = priceMatcher.group(1).replaceAll("[^0-9]", "");
                if (!priceStr.isEmpty()) {
                    totalPrice = Long.parseLong(priceStr);
                }
            }

            if (totalPrice == 0L) {
                for (JsonNode field : fields) {
                    String rawText = field.path("inferText").asText("").trim();
                    if (isPossiblePrice(rawText)) {
                        Long tempPrice = Long.parseLong(rawText.replaceAll("[^0-9]", ""));
                        if (tempPrice > totalPrice) {
                            totalPrice = tempPrice;
                        }
                    }
                }
            }

            for (JsonNode field : fields) {
                JsonNode inferConfidence = field.get("inferConfidence");
                if (inferConfidence != null && inferConfidence.isNumber()) {
                    confidenceSum += inferConfidence.asDouble();
                    confidenceCount++;
                }
            }
            double confidence = confidenceCount == 0 ? 0.0 : confidenceSum / confidenceCount;
            if ("알 수 없는 상호명".equals(storeName) || totalPrice <= 0 || paymentDate == null) {
                confidence = Math.min(confidence, 0.5);
            }

            return OcrResponse.builder()
                    .storeName(storeName)
                    .amount(totalPrice)
                    .paymentDate(paymentDate)
                    .confidence(confidence)
                    .correctedByLlm(false)
                    .build();

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("OCR 결과 파싱 실패: ", e);
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String normalizeStoreName(String value) {
        return value
                .replaceAll("\\s*[/|]?\\s*\\d{3}-\\d{2}-\\d{5}\\s*$", "")
                .replaceAll("\\s*\\([A-Za-z][A-Za-z\\s.-]*\\)\\s*$", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isMeaningfulStoreName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String comparable = value
                .replaceAll("[^가-힣A-Za-z0-9]", "")
                .toLowerCase();
        if (comparable.length() < 2 || RECEIPT_HEADER_WORDS.contains(comparable)) {
            return false;
        }
        return !comparable.startsWith("사업자번호")
                && !comparable.startsWith("대표자")
                && !comparable.startsWith("전화")
                && !comparable.startsWith("주소")
                && !comparable.startsWith("판매시간")
                && !comparable.startsWith("승인번호");
    }

    private boolean isPossiblePrice(String text) {
        if (text.isBlank()
                || text.contains(":")
                || text.matches(".*\\d{2,4}[-./]\\d{1,2}[-./]\\d{1,2}.*")
                || text.matches(".*\\d{2,4}-\\d{2,4}-\\d{4}.*")
                || text.matches(".*(?:사업자|전화|승인|카드|일시).*")) {
            return false;
        }

        String digits = text.replaceAll("[^0-9]", "");
        if (digits.length() < 3 || digits.length() > 8) {
            return false;
        }
        long amount = Long.parseLong(digits);
        return amount > 0
                && amount <= 100_000_000L
                && (text.contains(",") || text.matches(".*\\d\\s*원.*"));
    }
}
