package com.travelbird.user.controller;

import com.travelbird.user.dto.request.KakaoLoginRequest;
import com.travelbird.user.dto.response.KakaoLoginResponse;
import com.travelbird.user.dto.request.RefreshTokenRequest;
import com.travelbird.user.dto.response.TokenPair;
import com.travelbird.user.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/kakao/login")
    public ResponseEntity<KakaoLoginResponse> kakaoLogin(@RequestBody(required = false) KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody(required = false) RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }
}
