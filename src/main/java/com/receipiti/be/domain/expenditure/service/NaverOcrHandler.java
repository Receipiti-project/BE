package com.receipiti.be.domain.expenditure.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
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

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class NaverOcrHandler {

    @Value("${naver.ocr.url}")
    private String naverOcrUrl;

    @Value("${naver.ocr.secret}")
    private String naverOcrSecret;

    public OcrResponse executeOcr(MultipartFile file) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("X-OCR-SECRET", naverOcrSecret);

            String jsonMessage = String.format(
                    "{\"images\":[{\"format\":\"%s\",\"name\":\"receipt\"}],\"requestId\":\"%s\",\"timestamp\":%d,\"version\":\"V2\"}",
                    getFileExtension(file.getOriginalFilename()),
                    UUID.randomUUID().toString(),
                    System.currentTimeMillis()
            );

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
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(naverOcrUrl, requestEntity, String.class);

            return parseOcrResponse(responseEntity.getBody());

        } catch (Exception e) {
            log.error("Naver OCR API 호출 실패: ", e);
            throw new RuntimeException("영수증 OCR 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private OcrResponse parseOcrResponse(String jsonResponseBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponseBody);
            JsonNode fields = root.path("images").get(0).path("fields");

            StringBuilder fullTextBuilder = new StringBuilder();
            if (fields.isArray()) {
                for (JsonNode field : fields) {
                    fullTextBuilder.append(field.path("inferText").asText("")).append(" ");
                }
            }
            String fullText = fullTextBuilder.toString().trim();

            String storeName = "알 수 없는 상호명";
            Long totalPrice = 0L;
            LocalDateTime paymentDate = LocalDateTime.now();

            // 상호명 추출
            if (fullText.contains("[매장명]") || fullText.contains("[가맹점명]")) {
                Pattern storePattern = Pattern.compile("(?:\\[매장명\\]|\\[가맹점명\\])\\s*:?\\s*([^\\s/]+)");
                Matcher storeMatcher = storePattern.matcher(fullText);
                if (storeMatcher.find()) {
                    storeName = storeMatcher.group(1).trim();
                }
            } else if (fields.size() > 0) {
                for (JsonNode field : fields) {
                    String firstText = field.path("inferText").asText("").trim();
                    if (!firstText.equals("[영수증]") && !firstText.equals("영수증") && !firstText.isEmpty()) {
                        storeName = firstText;
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
                paymentDate = LocalDateTime.of(year, month, day, hour, minute, second);
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
                    String text = field.path("inferText").asText("").replaceAll("[^0-9]", "");
                    if (!text.isEmpty() && text.length() >= 4 && text.length() <= 7) {
                        Long tempPrice = Long.parseLong(text);
                        if (tempPrice > totalPrice && tempPrice != 191207L) {
                            totalPrice = tempPrice;
                        }
                    }
                }
            }

            return OcrResponse.builder()
                    .storeName(storeName)
                    .amount(totalPrice)
                    .paymentDate(paymentDate)
                    .build();

        } catch (Exception e) {
            log.error("OCR 결과 파싱 실패: ", e);
            return OcrResponse.builder()
                    .storeName("영수증 분석 실패")
                    .amount(0L)
                    .paymentDate(LocalDateTime.now())
                    .build();
        }
    }
}