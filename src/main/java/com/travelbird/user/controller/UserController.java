package com.travelbird.user.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.personality.dto.response.PartnerBirdResponse;
import com.travelbird.personality.service.OnboardingService;
import com.travelbird.user.service.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final WithdrawalService withdrawalService;
    private final OnboardingService onboardingService;

    public UserController(WithdrawalService withdrawalService, OnboardingService onboardingService) {
        this.withdrawalService = withdrawalService;
        this.onboardingService = onboardingService;
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw() {
        withdrawalService.withdraw(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/partner-bird")
    public ResponseEntity<PartnerBirdResponse> partnerBird() {
        return ResponseEntity.ok(onboardingService.getPartnerBird(SecurityUtils.getCurrentUserId()));
    }
}
