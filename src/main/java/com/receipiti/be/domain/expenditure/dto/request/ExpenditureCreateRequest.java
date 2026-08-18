package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class ExpenditureCreateRequest{
    @jakarta.validation.constraints.NotNull
    private Long categoryId;
    @jakarta.validation.constraints.NotBlank
    private String storeName;
    @Size(max = 100, message = "업종명은 100자 이하로 입력해주세요.")
    private String businessCategory;
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.Positive
    private Long amount;
    @jakarta.validation.constraints.NotNull
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
}
