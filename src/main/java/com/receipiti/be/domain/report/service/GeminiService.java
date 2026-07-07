package com.receipiti.be.domain.report.service;

import com.receipiti.be.domain.report.dto.request.GeminiRequest;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public ReportResponse generateExpenditureReport(String targetMonth, String expenditureData) {
        // 구글 AI 스튜디오 표준 API 최신 엔드포인트 주소
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;
        System.out.println("Gemini API Key prefix = " + apiKey.substring(0, 10));

        String prompt = String.format(
                "너는 유저의 가계부 소비 내역을 분석해주는 전문 자산 관리사야. [%s]의 소비 내역 데이터를 바탕으로 반드시 아래 5가지 항목을 모두 포함해서 구체적인 리포트를 작성해줘.\n\n" +
                        "1. 카테고리별 소비 분석\n2. 요일별 소비 분석\n3. 시간대별 소비 분석\n4. 소비 습관 진단\n5. 이상 소비 탐지\n\n" +
                        "말투는 부드러운 격식체(~합니다)를 사용하고, 가독성을 위해 마크다운 문법(##, *, -)을 지켜줘.\n\n데이터:\n%s",
                targetMonth, expenditureData
        );

        GeminiRequest request = new GeminiRequest(prompt);

        try {
            // 구글 표준 API 호출
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            // 4단계 계층형 JSON 텍스트 파싱 로직
            if (response != null && response.containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    if (firstCandidate.containsKey("content")) {
                        Map<String, Object> content = (Map<String, Object>) firstCandidate.get("content");
                        if (content.containsKey("parts")) {
                            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                            if (!parts.isEmpty()) {
                                String aiText = (String) parts.get(0).get("text");
                                return new ReportResponse(aiText); // 🎯 성공 반환!
                            }
                        }
                    }
                }
            }

            return new ReportResponse("AI 응답 포맷을 파싱할 수 없습니다. 응답 구조를 확인해 주세요.");

        } catch (HttpClientErrorException e) {
            System.out.println("======  구글 제미나이 API 에러 발생! ======");
            System.out.println("에러 코드: " + e.getStatusCode());
            System.out.println("구글 메시지: " + e.getResponseBodyAsString());
            return new ReportResponse("구글 API 에러: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return new ReportResponse("AI 리포트를 생성하는 중 내부 시스템 오류가 발생했습니다.");
        }
    }
}