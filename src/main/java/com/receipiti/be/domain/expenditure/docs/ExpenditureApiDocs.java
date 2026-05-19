package com.receipiti.be.domain.expenditure.docs;

import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureDetailResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Expenditures", description = "지출 내역 관련 API")
public interface ExpenditureApiDocs {

    @Operation(summary = "지출 내역 수동 입력", description = "사용자가 직접 지출 내역을 입력하여 저장합니다")
    ResponseEntity<ExpenditureCreateResponse> createExpenditure(
            Member member,
            ExpenditureCreateRequest request
    );

    @Operation(summary = "지출 내역 목록 조회", description = "특정 년도와 월을 입력받아 해당 기간의 지출 내역 목록을 반환합니다.")
    @Parameters({
            @Parameter(name = "year", description = "조회할 년도", example = "2026"),
            @Parameter(name = "month", description = "조회할 월", example = "5")
    })
    ResponseEntity<ExpenditureListResponse> getExpenditures(
            Member member,
            int year,
            int month
    );

    @Operation(summary = "지출 내역 상세 조회", description = "특정 지출 내역의 상세 정보를 조회합니다.")
    @Parameter(name = "id", description = "조회할 지출 내역의 id", example = "1")
    ResponseEntity<ExpenditureDetailResponse> getExpenditureDetail(
            Member member,
            Long expenditureId
    );
}