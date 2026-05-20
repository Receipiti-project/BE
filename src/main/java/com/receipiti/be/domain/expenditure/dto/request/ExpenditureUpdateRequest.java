package com.receipiti.be.domain.expenditure.dto.request;

import com.receipiti.be.domain.expenditure.enums.Currency;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class ExpenditureUpdateRequest {
    private Long categoryId;
    @Size(max = 50, message = "상호명은 50자 이하로 입력해주세요.")
    private String storeName;
    @PositiveOrZero(message = "지출 금액은 0원 이상이어야 합니다.")
    private Long amount;
    private LocalDateTime expenditureDate;
    @Size(max = 255, message = "메모는 255자 이하로 입력해주세요.")
    private String memo;
    private Currency currency;
}