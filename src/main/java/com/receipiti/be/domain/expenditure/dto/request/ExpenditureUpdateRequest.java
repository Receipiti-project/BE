package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ExpenditureUpdateRequest {
    private Long categoryId;
    private String storeName;
    private Long amount;
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}