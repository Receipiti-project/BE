package com.receipiti.be.domain.report.service;

import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.report.dto.response.ReportResponse;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("uuuu-MM-dd HH:mm");
    private static final String NO_INFORMATION = "정보 없음";

    private final ExpenditureRepository expenditureRepository;
    private final GeminiService geminiService;

    public ReportResponse createReport(Member member, String targetMonth) {
        YearMonth yearMonth = parseTargetMonth(targetMonth);
        LocalDateTime start = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime end = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        List<Expenditure> expenditures = expenditureRepository.findByMonth(member, start, end);
        String expenditureData = buildExpenditureData(targetMonth, expenditures);
        return geminiService.generateExpenditureReport(targetMonth, expenditureData);
    }

    private YearMonth parseTargetMonth(String targetMonth) {
        try {
            return YearMonth.parse(targetMonth, MONTH_FORMATTER);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
    }

    private String buildExpenditureData(String targetMonth, List<Expenditure> expenditures) {
        StringBuilder data = new StringBuilder()
                .append("분석 대상 월: ").append(targetMonth).append('\n')
                .append("지출 내역 건수: ").append(expenditures.size()).append('\n')
                .append("지출일시\t금액\t통화\t카테고리\t가맹점\t업종\t메모\n");

        if (expenditures.isEmpty()) {
            return data.append("(지출 내역 없음)").toString();
        }

        expenditures.forEach(expenditure -> appendExpenditure(data, expenditure));
        return data.toString();
    }

    private void appendExpenditure(StringBuilder data, Expenditure expenditure) {
        Store store = expenditure.getStore();
        data.append(expenditure.getExpenditureDate().format(DATE_TIME_FORMATTER)).append('\t')
                .append(expenditure.getAmount()).append('\t')
                .append(expenditure.getCurrency() == null ? NO_INFORMATION : expenditure.getCurrency()).append('\t')
                .append(sanitize(expenditure.getCategory().getName())).append('\t')
                .append(sanitize(store.getName())).append('\t')
                .append(sanitize(store.getBizCategory())).append('\t')
                .append(sanitize(expenditure.getMemo())).append('\n');
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return NO_INFORMATION;
        }
        return value.trim()
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
