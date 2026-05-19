package com.receipiti.be.domain.expenditure.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExpenditureListResponse {
    private Long totalAmount;
    private List<DailyExpenditureGroup> dailyExpenditures;
}
