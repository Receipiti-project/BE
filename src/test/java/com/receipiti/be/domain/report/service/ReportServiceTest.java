package com.receipiti.be.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.receipiti.be.domain.category.entity.Category;
import com.receipiti.be.domain.expenditure.entity.Expenditure;
import com.receipiti.be.domain.expenditure.enums.Currency;
import com.receipiti.be.domain.expenditure.repository.ExpenditureRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.store.entity.Store;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ExpenditureRepository expenditureRepository;

    @Mock
    private GeminiService geminiService;

    @InjectMocks
    private ReportService reportService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder().id(1L).build();
    }

    @Test
    void 로그인_사용자의_해당_월_지출을_DB에서_조회해_분석한다() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 0, 0);
        given(expenditureRepository.findByMonth(member, start, end))
                .willReturn(List.of(expenditure()));

        reportService.createReport(member, "2026-09");

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateExpenditureReport(
                org.mockito.ArgumentMatchers.eq("2026-09"),
                dataCaptor.capture()
        );
        assertThat(dataCaptor.getValue())
                .contains("지출 내역 건수: 1")
                .contains("2026-09-04 18:30\t12000\tKRW\t식비\t홍대 식당\t한식\t저녁 식사");
    }

    @Test
    void 지출이_없는_달은_빈_내역임을_Gemini에_전달한다() {
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 1, 0, 0);
        given(expenditureRepository.findByMonth(member, start, end)).willReturn(List.of());

        reportService.createReport(member, "2026-09");

        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(geminiService).generateExpenditureReport(
                org.mockito.ArgumentMatchers.eq("2026-09"),
                dataCaptor.capture()
        );
        assertThat(dataCaptor.getValue())
                .contains("지출 내역 건수: 0")
                .contains("(지출 내역 없음)");
    }

    @Test
    void 분석월_형식이_잘못되면_DB와_Gemini를_호출하지_않는다() {
        assertThatThrownBy(() -> reportService.createReport(member, "2026-13"))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.BAD_REQUEST);

        verify(expenditureRepository, never()).findByMonth(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(geminiService, never()).generateExpenditureReport(anyString(), anyString());
    }

    private Expenditure expenditure() {
        Category category = Category.builder().id(1L).name("식비").build();
        Store store = Store.builder()
                .id(1L)
                .name("홍대 식당")
                .bizCategory("한식")
                .build();
        return Expenditure.builder()
                .id(1L)
                .member(member)
                .category(category)
                .store(store)
                .amount(12_000L)
                .currency(Currency.KRW)
                .memo("저녁 식사")
                .expenditureDate(LocalDateTime.of(2026, 9, 4, 18, 30))
                .build();
    }
}
