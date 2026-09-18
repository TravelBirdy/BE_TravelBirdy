package com.travelbird.user.client;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoApiClient {

    private static final int TIMEOUT_MILLIS = 3000;

    private final RestClient restClient;
    private final String userInfoUri;

    public KakaoApiClient(@Value("${kakao.user-info-uri}") String userInfoUri) {
        this.userInfoUri = userInfoUri;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(TIMEOUT_MILLIS);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    public KakaoUserInfo getUserInfo(String kakaoAccessToken) {
        try {
            KakaoUserInfoResponse response = restClient.get()
                    .uri(userInfoUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .onStatus(status -> status.value() == 401,
                            (req, res) -> {
                                throw new BusinessException(ErrorCode.KAKAO_AUTHENTICATION_FAILED);
                            })
                    .onStatus(HttpStatusCode::isError,
                            (req, res) -> {
                                throw new BusinessException(ErrorCode.KAKAO_SERVICE_UNAVAILABLE);
                            })
                    .body(KakaoUserInfoResponse.class);
            if (response == null || response.id() == null) {
                throw new BusinessException(ErrorCode.KAKAO_AUTHENTICATION_FAILED);
            }
            return response.toKakaoUserInfo();
        } catch (BusinessException e) {
            throw e;
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.KAKAO_SERVICE_UNAVAILABLE);
        }
    }
}
