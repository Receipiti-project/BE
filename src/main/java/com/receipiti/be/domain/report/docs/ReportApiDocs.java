package com.receipiti.be.domain.report.docs;

import com.receipiti.be.domain.report.dto.request.ReportCreateRequest;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "AI Report API", description = "소비 데이터를 분석하여 AI 리포트를 생성하는 API입니다.")
public interface ReportApiDocs {

    @Operation(
            summary = "AI 소비 분석 리포트 생성",
            description = "선택한 월과 해당 월의 소비 데이터를 기반으로 총 지출, 이상 소비 탐지, 주요 시간대·요일·카테고리, 소비 패턴과 요약을 구조화된 JSON으로 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "AI 리포트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ReportResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청 월 형식 또는 소비 데이터 검증 실패",
                    content = @Content(schema = @Schema(
                            implementation = com.receipiti.be.global.apiPayload.ApiResponse.class))),
            @ApiResponse(responseCode = "422", description = "Gemini 응답 형식 또는 분석 결과 검증 실패",
                    content = @Content(schema = @Schema(
                            implementation = com.receipiti.be.global.apiPayload.ApiResponse.class))),
            @ApiResponse(responseCode = "502", description = "구글 Gemini API 호출 실패",
                    content = @Content(schema = @Schema(
                            implementation = com.receipiti.be.global.apiPayload.ApiResponse.class)))
    })
    ReportResponse createReport(
            @Parameter(description = "분석 대상 월", example = "2026-06")
            @RequestParam String month,

            @Parameter(description = "분석할 가계부 소비 내역 텍스트 데이터", example = "- 6/5 식비 12000원...")
            @Valid @RequestBody ReportCreateRequest reportCreateRequest
    );
}
