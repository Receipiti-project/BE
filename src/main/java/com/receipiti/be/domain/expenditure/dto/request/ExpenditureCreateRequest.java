package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ExpenditureCreateRequest {
    private Long categoryId;
    private Long defaultCategoryId;
    @NotBlank
    private String storeName;
    @Size(max = 100, message = "업종명은 100자 이하로 입력해주세요.")
    private String businessCategory;
    @NotNull
    @Positive
    private Long amount;
    @NotNull
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;

    @JsonIgnore
    @AssertTrue(message = "categoryId 또는 defaultCategoryId 중 하나는 필수입니다.")
    public boolean isCategorySelectionValid() {
        return categoryId != null || defaultCategoryId != null;
    }
}
