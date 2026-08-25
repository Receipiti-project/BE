package com.receipiti.be.domain.expenditure.docs;

import com.receipiti.be.domain.expenditure.dto.request.ExpenditureCreateRequest;
import com.receipiti.be.domain.expenditure.dto.request.ExpenditureUpdateRequest;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureCreateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureDetailResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureListResponse;
import com.receipiti.be.domain.expenditure.dto.response.ExpenditureUpdateResponse;
import com.receipiti.be.domain.expenditure.dto.response.ConsumptionRouteResponse;
import com.receipiti.be.domain.expenditure.dto.response.OcrResponse;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;

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

    @Operation(
            summary = "일별 소비 동선 조회",
            description = "선택한 날짜의 좌표가 저장된 지출을 방문 시각순으로 조회하고 직선 기준 예상 이동거리를 반환합니다."
    )
    ResponseEntity<ConsumptionRouteResponse> getConsumptionRoute(
            @Parameter(hidden = true) Member member,
            @Parameter(description = "조회 날짜", example = "2026-08-25") LocalDate date
    );

    @Operation(summary = "지출 내역 상세 조회", description = "특정 지출 내역의 상세 정보를 조회합니다.")
    @Parameter(name = "id", description = "조회할 지출 내역의 id", example = "1")
    ResponseEntity<ExpenditureDetailResponse> getExpenditureDetail(
            Member member,
            Long id
    );

    @Operation(summary = "지출 내역 수정", description = "특정 지출 내역의 정보를 부분 수정합니다.")
    @Parameter(name = "id", description = "수정할 지출 내역의 id", example = "1")
    ResponseEntity<ExpenditureUpdateResponse> updateExpenditure(
            Member member,
            Long id,
            ExpenditureUpdateRequest request
    );

    @Operation(summary = "지출 내역 삭제", description = "특정 지출 내역을 삭제합니다.")
    @Parameter(name = "id", description = "삭제할 지출 내역의 id", example = "1")
    ResponseEntity<String> deleteExpenditure(
            Member member,
            Long id
    );

    @Operation(summary = "영수증 OCR 텍스트 추출", description = "영수증 사진을 받아 상호명, 금액, 날짜를 추출합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(description = "추출된 영수증 가계부 데이터를 반환합니다.")
    ResponseEntity<OcrResponse> extractTextFromReceipt(
            org.springframework.web.multipart.MultipartFile file
    );
}
