package com.receipiti.be.domain.expenditure.dto.response;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DailyExpenditureGroup {
    private LocalDate date;
    private Long dailyTotalAmount;
    private List<ExpenditureElement> list;
}
