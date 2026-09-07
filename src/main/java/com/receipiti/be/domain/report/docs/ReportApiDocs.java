package com.receipiti.be.domain.report.docs;

import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "AI Report API", description = "소비 데이터를 분석하여 AI 리포트를 생성하는 API입니다.")
public interface ReportApiDocs {

    @Operation(
            summary = "AI 소비 분석 리포트 생성",
            description = "로그인 사용자의 선택한 월 지출 내역을 DB에서 조회하여 총 지출, 이상 소비 탐지, 주요 시간대·요일·카테고리, 소비 패턴과 요약을 구조화된 JSON으로 생성합니다."
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
            @Parameter(hidden = true)
            @AuthenticationPrincipal Member member,

            @Parameter(description = "분석 대상 월", example = "2026-06")
            @RequestParam String month
    );
}
