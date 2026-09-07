package com.receipiti.be.domain.report.controller;

import com.receipiti.be.domain.report.docs.ReportApiDocs;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import com.receipiti.be.domain.report.service.ReportService;
import com.receipiti.be.domain.member.entity.Member;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ReportController implements ReportApiDocs {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    @PostMapping("/report")
    public ReportResponse createReport(
            @AuthenticationPrincipal Member member,
            @RequestParam String month) {
        return reportService.createReport(member, month);
    }
}
