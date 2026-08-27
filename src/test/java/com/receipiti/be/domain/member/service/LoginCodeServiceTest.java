package com.receipiti.be.domain.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.receipiti.be.domain.member.dto.response.LoginResponse;
import com.receipiti.be.domain.member.entity.LoginCode;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.enums.SocialType;
import com.receipiti.be.domain.member.repository.LoginCodeRepository;
import com.receipiti.be.domain.member.repository.MemberRepository;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import com.receipiti.be.global.auth.provider.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginCodeServiceTest {

    @Mock
    private LoginCodeRepository loginCodeRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private LoginCodeService loginCodeService;

    @BeforeEach
    void setUp() {
        loginCodeService = new LoginCodeService(loginCodeRepository, memberRepository, jwtTokenProvider);
        ReflectionTestUtils.setField(loginCodeService, "expirationSeconds", 120L);
    }

    @Test
    void 발급한_로그인_코드는_한_번만_JWT로_교환된다() {
        Member member = kakaoMember();
        AtomicReference<LoginCode> savedCode = new AtomicReference<>();
        when(memberRepository.findBySocialIdAndSocialType(123L, SocialType.KAKAO))
                .thenReturn(Optional.of(member));
        when(loginCodeRepository.save(any(LoginCode.class))).thenAnswer(invocation -> {
            LoginCode loginCode = invocation.getArgument(0);
            savedCode.set(loginCode);
            return loginCode;
        });
        when(jwtTokenProvider.createToken("123")).thenReturn("service-jwt");

        String rawCode = loginCodeService.issue(123L);
        when(loginCodeRepository.findByCodeHashForUpdate(savedCode.get().getCodeHash()))
                .thenReturn(Optional.of(savedCode.get()));

        LoginResponse response = loginCodeService.exchange(rawCode);

        assertThat(response.accessToken()).isEqualTo("service-jwt");
        assertThat(savedCode.get().getConsumedAt()).isNotNull();
        assertThatThrownBy(() -> loginCodeService.exchange(rawCode))
                .isInstanceOf(GeneralException.class);
    }

    @Test
    void 만료된_로그인_코드는_교환할_수_없다() {
        LoginCode expiredCode = LoginCode.builder()
                .codeHash("expired-hash")
                .member(kakaoMember())
                .expiresAt(LocalDateTime.now().minusSeconds(1))
                .build();
        when(loginCodeRepository.findByCodeHashForUpdate(any(String.class)))
                .thenReturn(Optional.of(expiredCode));

        assertThatThrownBy(() -> loginCodeService.exchange("expired-code"))
                .isInstanceOf(GeneralException.class);
    }

    private Member kakaoMember() {
        return Member.builder()
                .id(1L)
                .socialId(123L)
                .socialType(SocialType.KAKAO)
                .nickname("테스터")
                .email("test@example.com")
                .build();
    }
}
