package com.receipiti.be.domain.expenditure.dto.response;

import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.enums.InputType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExpenditureDetailResponse {
    private Long expenditureId;
    private Long categoryId;
    private String categoryName;
    private String storeName;
    private Long amount;
    private LocalDateTime expenditureDate;
    private String memo;
    private Currency currency;
    private InputType inputType; // 수동 입력 or 영수증 인식
    private LocalDateTime createdAt; // 등록일
}