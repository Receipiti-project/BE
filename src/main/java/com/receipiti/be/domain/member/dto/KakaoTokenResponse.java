package com.receipiti.be.domain.member.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoTokenResponse(@JsonProperty("access_token") String accessToken) {
}
