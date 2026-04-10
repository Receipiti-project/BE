package com.receipiti.be.global.config;

import com.receipiti.be.domain.member.service.CustomOAuth2UserService;
import com.receipiti.be.global.auth.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2UserService oAuth2UserService;

    // 인증 없이 접근 가능한 주소들
    private final String[] allowUris = {
            "/swagger-ui/**",
            "/v1/api-docs/**",
            "/error",
            "/login/**",
            "/oauth2/**",
            "/api/v1/auth/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OAuth2SuccessHandler oAuth2SuccessHandler) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)    // CSRF 끄기
                .formLogin(AbstractHttpConfigurer::disable) // 기본 폼 로그인 끄기
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 끄기

                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowUris).permitAll() // 허용 리스트는 아무나 접근 가능
                        .anyRequest().authenticated()           // 나머지는 인증 필요
                )

                // 카카오 로그인을 위한 OAuth2 설정 추가
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessUrl("/api/v1/auth/logout?logout") // 임시 로그아웃 성공 주소
                        .permitAll()
                );

        return http.build();
    }

    // 비밀번호 암호화 빈 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}