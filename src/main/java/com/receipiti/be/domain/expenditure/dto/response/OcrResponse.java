package com.receipiti.be.domain.expenditure.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResponse {
    private String storeName;
    private Long amount;
    private LocalDateTime paymentDate;
    private Double confidence;
    private Boolean correctedByLlm;
}
