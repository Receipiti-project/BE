package com.receipiti.be.domain.report.controller;

import com.receipiti.be.domain.report.docs.ReportApiDocs;
import com.receipiti.be.domain.report.dto.request.ReportCreateRequest;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import com.receipiti.be.domain.report.service.GeminiService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReportController implements ReportApiDocs {

    private final GeminiService geminiService;

    public ReportController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @Override
    @PostMapping("/report")
    public ReportResponse createReport(
            @RequestParam String month,
            @RequestBody ReportCreateRequest request) {

        // 실제로는 DB에서 해당 월의 회원의 지출 데이터를 꺼내와서 넘겨주는 로직이 들어갈 예정입니다.
        // 지금은 테스트를 위해 가상의 데이터를 넣었습니다.
        return geminiService.generateExpenditureReport(month, request.getExpenditureData());
    }
}