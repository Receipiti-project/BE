package com.receipiti.be.domain.expenditure.dto.response;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExpenditureCreateResponse {
    private Long expenditureId;
    private String storeName;
    private Long amount;
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}
