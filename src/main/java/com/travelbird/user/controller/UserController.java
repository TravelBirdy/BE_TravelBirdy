package com.travelbird.user.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.mypage.dto.response.MyPageResponse;
import com.travelbird.mypage.service.MyPageService;
import com.travelbird.personality.dto.response.PartnerBirdResponse;
import com.travelbird.personality.service.OnboardingService;
import com.travelbird.user.dto.response.ProfileResponse;
import com.travelbird.user.dto.request.UpdateProfileRequest;
import com.travelbird.user.service.ProfileService;
import com.travelbird.user.service.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final WithdrawalService withdrawalService;
    private final OnboardingService onboardingService;
    private final ProfileService profileService;
    private final MyPageService myPageService;

    public UserController(
            WithdrawalService withdrawalService,
            OnboardingService onboardingService,
            ProfileService profileService,
            MyPageService myPageService
    ) {
        this.withdrawalService = withdrawalService;
        this.onboardingService = onboardingService;
        this.profileService = profileService;
        this.myPageService = myPageService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getProfile() {
        return ResponseEntity.ok(profileService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PatchMapping("/me/profile")
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody(required = false) UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateProfile(SecurityUtils.getCurrentUserId(), request));
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

    @GetMapping("/me/mypage")
    public ResponseEntity<MyPageResponse> myPage() {
        return ResponseEntity.ok(myPageService.getMyPage(SecurityUtils.getCurrentUserId()));
    }
}
