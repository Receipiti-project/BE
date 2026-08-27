package com.receipiti.be.domain.member.docs;

import com.receipiti.be.domain.member.dto.request.KakaoLoginRequest;
import com.receipiti.be.domain.member.dto.request.LoginCodeExchangeRequest;
import com.receipiti.be.domain.member.dto.response.LoginResponse;
import com.receipiti.be.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 관련 API")
public interface AuthApiDocs {

    @Operation(
            summary = "카카오 로그인",
            description = "프론트에서 발급받은 카카오 인가 코드를 자체 서비스의 JWT로 교환합니다. "
                    + "redirectUri는 카카오 인가 요청에 사용한 URI와 정확히 같아야 합니다.")
    ResponseEntity<ApiResponse<LoginResponse>> loginWithKakao(@RequestBody KakaoLoginRequest request);

    @Operation(
            summary = "모바일 로그인 코드 교환",
            description = "카카오 로그인 성공 후 앱 딥링크로 전달받은 일회용 loginCode를 자체 서비스의 JWT로 교환합니다.")
    ResponseEntity<ApiResponse<LoginResponse>> exchangeLoginCode(@RequestBody LoginCodeExchangeRequest request);
}
