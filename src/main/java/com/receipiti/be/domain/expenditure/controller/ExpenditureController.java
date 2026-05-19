package com.receipiti.be.domain.expenditure.controller;

import com.receipiti.be.domain.expenditure.docs.ExpenditureApiDocs;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureDetailResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.expenditure.service.ExpenditureService;
import com.receipiti.be.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/expenditures")
@RequiredArgsConstructor
public class ExpenditureController implements ExpenditureApiDocs {
    private final ExpenditureService expenditureService;

    @Override
    @PostMapping
    public ResponseEntity<ExpenditureCreateResponse> createExpenditure(
            @AuthenticationPrincipal Member member,
            @RequestBody @Valid ExpenditureCreateRequest request) {

        log.info("request: {}", request);
        ExpenditureCreateResponse response = expenditureService.createExpenditure(member, request);

        return ResponseEntity.status(201).body(response);
    }

    @Override
    @GetMapping
    public ResponseEntity<ExpenditureListResponse> getExpenditures(
            @AuthenticationPrincipal Member member,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("지출 목록 조회 요청 - 년도: {}, 월: {}", year, month);
        ExpenditureListResponse response = expenditureService.getExpenditureList(member, year, month);

        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ExpenditureDetailResponse> getExpenditureDetail(
            @AuthenticationPrincipal Member member,
            @PathVariable Long id) {

        log.info("지출 상세 조회 요청 - 유저: {}, 지출ID: {}", member.getNickname(), id);
        ExpenditureDetailResponse response = expenditureService.getExpenditureDetail(member, id);

        return ResponseEntity.ok(response);
    }
}
