package com.receipiti.be.domain.expenditure.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CardNotificationAnalysisResponse(
        boolean paymentNotification,
        String cardCompany,
        String storeName,
        Long amount,
        String paymentDateTime,
        String currency,
        String approvalStatus,
        Double confidence
) {
}
