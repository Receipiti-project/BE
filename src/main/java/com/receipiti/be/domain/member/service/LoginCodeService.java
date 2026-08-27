package com.receipiti.be.domain.member.service;

import com.receipiti.be.domain.member.dto.response.LoginResponse;
import com.receipiti.be.domain.member.entity.LoginCode;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.enums.SocialType;
import com.receipiti.be.domain.member.repository.LoginCodeRepository;
import com.receipiti.be.domain.member.repository.MemberRepository;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import com.receipiti.be.global.auth.provider.JwtTokenProvider;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int CODE_BYTES = 32;

    private final LoginCodeRepository loginCodeRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final Clock clock = Clock.systemDefaultZone();

    @Value("${oauth2.login-code-expiration-seconds:120}")
    private long expirationSeconds;

    @Transactional
    public String issue(Long socialId) {
        Member member = memberRepository.findBySocialIdAndSocialType(socialId, SocialType.KAKAO)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.UNAUTHORIZED));

        byte[] randomBytes = new byte[CODE_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawCode = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime now = LocalDateTime.now(clock);

        loginCodeRepository.save(LoginCode.builder()
                .codeHash(hash(rawCode))
                .member(member)
                .expiresAt(now.plusSeconds(expirationSeconds))
                .build());

        return rawCode;
    }

    @Transactional
    public LoginResponse exchange(String rawCode) {
        LoginCode loginCode = loginCodeRepository.findByCodeHashForUpdate(hash(rawCode))
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INVALID_LOGIN_CODE));
        LocalDateTime now = LocalDateTime.now(clock);

        if (!loginCode.isUsableAt(now)) {
            throw new GeneralException(GeneralErrorCode.INVALID_LOGIN_CODE);
        }

        loginCode.consume(now);
        String accessToken = jwtTokenProvider.createToken(loginCode.getMember().getSocialId().toString());
        return new LoginResponse(accessToken);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exception);
        }
    }
}
