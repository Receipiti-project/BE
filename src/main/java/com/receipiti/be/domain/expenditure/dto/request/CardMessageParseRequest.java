package com.receipiti.be.domain.expenditure.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CardMessageParseRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(example = "[신한카드] 09/11 18:30 스타벅스 5,500원 승인")
        String message,

        @NotNull
        @Schema(example = "2026-09-11T18:30:10")
        LocalDateTime receivedAt,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "iOS 단축어 등 호출 측에서 생성한 고유 요청 ID", example = "ios-shortcut-550e8400")
        String externalId
) {
}
