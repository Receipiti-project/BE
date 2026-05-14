package com.receipiti.be.domain.member.dto;

import com.receipiti.be.domain.member.enums.SocialType;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OAuth2Attributes {
    private String email;
    private String nickname;

    public static OAuth2Attributes of(SocialType socialType, Map<String, Object> attributes) {
        return switch (socialType) {
            case KAKAO -> ofKakao(attributes);
        };
    }

    public static OAuth2Attributes ofKakao(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        return OAuth2Attributes.builder()
                .nickname((String) profile.get("nickname"))
                .email((String) kakaoAccount.get("email"))
                .build();
    }
}
