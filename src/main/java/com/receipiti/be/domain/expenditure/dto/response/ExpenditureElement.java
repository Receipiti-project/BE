package com.receipiti.be.domain.expenditure.dto.response;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpenditureElement {
    private Long expenditureId;
    private String categoryName;
    private String storeName;
    private String placeId;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Long amount;
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}
