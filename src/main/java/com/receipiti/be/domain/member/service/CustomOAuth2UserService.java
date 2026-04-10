package com.receipiti.be.domain.member.service;

import com.receipiti.be.domain.member.entity.Member;
import com.receipiti.be.domain.member.enums.SocialType;
import com.receipiti.be.domain.member.repository.MemberRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 소셜 서비스 구분
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        // 유저 정보 추출
        Map<String, Object> attributes = oAuth2User.getAttributes();
        Long socialId = (Long) attributes.get("id");

        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        String nickname = (String) profile.get("nickname");

        // 회원 저장 또는 업데이트
        Member member = saveOrUpdate(socialId, socialType, nickname, email);

        // 시큐리티 세션에 저장될 유저 정보 반환
        return oAuth2User;
    }

    private Member saveOrUpdate(Long socialId, SocialType socialType, String nickname, String email) {
        return memberRepository.findBySocialIdAndSocialType(socialId, socialType)
                .map(entity -> entity.update(nickname, email))
                .orElseGet(() -> memberRepository.save(Member.builder()
                        .socialId(socialId)
                        .socialType(socialType)
                        .nickname(nickname)
                        .email(email)
                        .build()));
    }
}