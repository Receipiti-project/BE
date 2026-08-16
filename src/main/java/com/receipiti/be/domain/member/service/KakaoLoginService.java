package com.receipiti.be.domain.member.service;

import com.receipiti.be.domain.member.dto.KakaoTokenResponse;
import com.receipiti.be.domain.member.dto.KakaoUserResponse;
import com.receipiti.be.domain.member.dto.request.KakaoLoginRequest;
import com.receipiti.be.domain.member.dto.response.LoginResponse;
import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.enums.SocialType;
import com.receipiti.be.domain.member.repository.MemberRepository;
import com.receipiti.be.global.apiPayload.code.GeneralErrorCode;
import com.receipiti.be.global.apiPayload.exception.GeneralException;
import com.receipiti.be.global.auth.provider.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

    private static final String KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URI = "https://kapi.kakao.com/v2/user/me";

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestClient kakaoRestClient;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String clientSecret;

    @Transactional
    public LoginResponse login(KakaoLoginRequest request) {
        try {
            String kakaoAccessToken = requestAccessToken(request);
            KakaoUserResponse kakaoUser = requestUserInfo(kakaoAccessToken);
            validateUserInfo(kakaoUser);

            Member member = saveOrUpdateMember(kakaoUser);

            return new LoginResponse(jwtTokenProvider.createToken(member.getSocialId().toString()));
        } catch (HttpClientErrorException exception) {
            throw new GeneralException(GeneralErrorCode.KAKAO_LOGIN_FAILED);
        } catch (HttpServerErrorException exception) {
            throw new GeneralException(GeneralErrorCode.KAKAO_SERVER_ERROR);
        } catch (ResourceAccessException exception) {
            throw new GeneralException(GeneralErrorCode.KAKAO_SERVICE_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private Member saveOrUpdateMember(KakaoUserResponse kakaoUser) {
        memberRepository.upsertSocialMember(
                kakaoUser.id(),
                SocialType.KAKAO.name(),
                kakaoUser.kakaoAccount().profile().nickname(),
                kakaoUser.kakaoAccount().email());

        return memberRepository.findBySocialIdAndSocialType(kakaoUser.id(), SocialType.KAKAO)
                .orElseThrow(() -> new GeneralException(GeneralErrorCode.INTERNAL_SERVER_ERROR));
    }

    private String requestAccessToken(KakaoLoginRequest request) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", request.redirectUri());
        form.add("code", request.authorizationCode());

        KakaoTokenResponse response = kakaoRestClient
                .post()
                .uri(KAKAO_TOKEN_URI)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(KakaoTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new GeneralException(GeneralErrorCode.KAKAO_LOGIN_FAILED);
        }
        return response.accessToken();
    }

    private KakaoUserResponse requestUserInfo(String accessToken) {
        KakaoUserResponse response = kakaoRestClient
                .get()
                .uri(KAKAO_USER_INFO_URI)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .body(KakaoUserResponse.class);

        if (response == null) {
            throw new GeneralException(GeneralErrorCode.KAKAO_LOGIN_FAILED);
        }
        return response;
    }

    private void validateUserInfo(KakaoUserResponse user) {
        if (user.id() == null || user.kakaoAccount() == null || user.kakaoAccount().profile() == null
                || user.kakaoAccount().email() == null || user.kakaoAccount().profile().nickname() == null) {
            throw new GeneralException(GeneralErrorCode.KAKAO_LOGIN_FAILED);
        }
    }
}
