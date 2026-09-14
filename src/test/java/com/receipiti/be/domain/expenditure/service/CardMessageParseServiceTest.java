package com.receipiti.be.domain.expenditure.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.receipiti.be.domain.expenditure.dto.request.CardMessageParseRequest;
import com.receipiti.be.domain.expenditure.dto.response.CardNotificationAnalysisResponse;
import com.receipiti.be.domain.expenditure.repository.CardMessageParseRequestRepository;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CardMessageParseServiceTest {

    private CardMessageParseRequestRepository repository;
    private CardMessageParseService service;
    private Member member;

    @BeforeEach
    void setUp() {
        repository = mock(CardMessageParseRequestRepository.class);
        service = new CardMessageParseService(repository);
        member = mock(Member.class);
    }

    @ParameterizedTest
    @MethodSource("approvalMessages")
    void 카드사별_승인_문자를_파싱한다(String message, String company, String storeName) {
        CardMessageParseRequest request = request(message, "request-1");

        CardNotificationAnalysisResponse response = service.parse(member, request);

        assertThat(response.paymentNotification()).isTrue();
        assertThat(response.cardCompany()).isEqualTo(company);
        assertThat(response.storeName()).isEqualTo(storeName);
        assertThat(response.amount()).isEqualTo(5_500L);
        assertThat(response.paymentDateTime()).isEqualTo("2026-09-11T18:30");
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
        verify(repository).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 승인_취소_문자를_파싱한다() {
        CardNotificationAnalysisResponse response = service.parse(
                member,
                request("[삼성카드] 09/11 18:30 스타벅스 5,500원 승인취소", "cancel-1")
        );

        assertThat(response.approvalStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void 신한카드_실제_문자_원문만으로_파싱한다() {
        String message = """
                [Web발신]
                [신한체크승인] 황*빈(1620) 09/04 21:57 (금액)54,000원 (주)씨브이코퍼레이션 남영
                """;

        CardNotificationAnalysisResponse response = service.parseRaw(member, message);

        assertThat(response.cardCompany()).isEqualTo("신한카드");
        assertThat(response.storeName()).isEqualTo("(주)씨브이코퍼레이션 남영");
        assertThat(response.amount()).isEqualTo(54_000L);
        assertThat(response.approvalStatus()).isEqualTo("APPROVED");
    }

    @Test
    void 연도_없는_미래_날짜는_직전_연도로_보정한다() {
        CardMessageParseRequest request = new CardMessageParseRequest(
                "[신한카드] 12/31 23:50 편의점 5,500원 승인",
                LocalDateTime.of(2027, 1, 1, 0, 1),
                "new-year-1"
        );

        CardNotificationAnalysisResponse response = service.parse(member, request);

        assertThat(response.paymentDateTime()).isEqualTo("2026-12-31T23:50");
    }

    @Test
    void 같은_사용자의_externalId가_중복되면_거절한다() {
        given(repository.existsByMemberAndExternalId(member, "duplicate-1")).willReturn(true);

        assertThatThrownBy(() -> service.parse(
                member,
                request("[신한카드] 09/11 18:30 스타벅스 5,500원 승인", "duplicate-1")
        ))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CARD_MESSAGE_DUPLICATE);
    }

    @Test
    void 카드_결제_문자가_아니면_거절하고_externalId를_저장하지_않는다() {
        assertThatThrownBy(() -> service.parse(member, request("택배가 도착했습니다.", "unsupported-1")))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(GeneralErrorCode.CARD_MESSAGE_UNSUPPORTED);

        verify(repository).existsByMemberAndExternalId(member, "unsupported-1");
        verify(repository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    private CardMessageParseRequest request(String message, String externalId) {
        return new CardMessageParseRequest(
                message,
                LocalDateTime.of(2026, 9, 11, 18, 30, 10),
                externalId
        );
    }

    private static Stream<Arguments> approvalMessages() {
        return Stream.of(
                Arguments.of("[신한카드] 09/11 18:30 스타벅스 5,500원 승인", "신한카드", "스타벅스"),
                Arguments.of("[KB국민카드] 09/11 18:30 편의점 5,500원 승인", "KB국민카드", "편의점"),
                Arguments.of("삼성카드 09/11 18:30 서점 5,500원 승인", "삼성카드", "서점")
        );
    }
}
