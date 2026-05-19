package com.receipiti.be.domain.expenditure.dto.response;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpenditureElement {
    private Long expenditureId;
    private String categoryName;
    private String storeName;
    private Long amount;
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}
