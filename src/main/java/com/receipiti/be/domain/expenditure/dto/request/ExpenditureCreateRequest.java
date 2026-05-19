package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExpenditureCreateRequest{
    @jakarta.validation.constraints.NotNull
    private Long categoryId;
    @jakarta.validation.constraints.NotBlank
    private String storeName;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long amount;
    @jakarta.validation.constraints.NotNull
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}