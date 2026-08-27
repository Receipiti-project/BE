package com.receipiti.be.global.auth.handler;

import com.receipiti.be.domain.member.service.LoginCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final LoginCodeService loginCodeService;

    @Value("${oauth2.redirect-url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // 로그인한 유저 정보 꺼내기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 카카오에서 준 고유 id를 꺼내기
        String socialId = oAuth2User.getAttribute("id").toString();

        String loginCode = loginCodeService.issue(Long.valueOf(socialId));

        // 프론트로 리다이렉트(임시주소)
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                .queryParam("loginCode", loginCode)
                .build()
                .encode()
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
