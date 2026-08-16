package com.receipiti.be.domain.member.controller;

import com.receipiti.be.domain.member.docs.AuthApiDocs;
import com.receipiti.be.domain.member.dto.request.KakaoLoginRequest;
import com.receipiti.be.domain.member.dto.response.LoginResponse;
import com.receipiti.be.domain.member.service.KakaoLoginService;
import com.receipiti.be.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApiDocs {

    private final KakaoLoginService kakaoLoginService;

    @Override
    @PostMapping("/login/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithKakao(
            @Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.onSuccess(kakaoLoginService.login(request)));
    }
}
