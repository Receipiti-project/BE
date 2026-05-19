package com.receipiti.be.domain.expenditure.controller;

import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.service.ExpenditureService;
import com.receipiti.be.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/expenditures")
@RequiredArgsConstructor
public class ExpenditureController {
    private final ExpenditureService expenditureService;

    @PostMapping
    public ResponseEntity<ExpenditureCreateResponse> createExpenditure(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid ExpenditureCreateRequest request) {

        log.info("request: {}", request);
        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        return ResponseEntity.status(201).body(response);
    }
}
