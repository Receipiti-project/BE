package com.receipiti.be.domain.expenditure.controller;

import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.service.ExpenditureService;
import com.receipiti.be.domain.member.entity.Member;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/expenditures")
@RequiredArgsConstructor
public class ExpenditureController {
    private final ExpenditureService expenditureService;

    @PostMapping
    public ResponseEntity<ExpenditureCreateResponse> createExpenditure(
            @AuthenticationPrincipal Member member,
            @RequestBody ExpenditureCreateRequest request,
            HttpServletResponse httpRequest) {

        System.out.println("request body: " + request);

        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        return ResponseEntity.ok(response);
    }
}
