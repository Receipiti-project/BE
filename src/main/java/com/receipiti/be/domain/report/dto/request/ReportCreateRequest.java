package com.receipiti.be.domain.report.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {

    @NotBlank(message = "소비 내역 데이터는 필수입니다.")
    private String expenditureData;
}
