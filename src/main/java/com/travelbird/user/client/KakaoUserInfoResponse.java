package com.travelbird.user.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoAccount(
            String email
    ) {
    }

    KakaoUserInfo toKakaoUserInfo() {
        String email = kakaoAccount == null ? null : kakaoAccount.email();
        return new KakaoUserInfo(id, email);
    }
}
