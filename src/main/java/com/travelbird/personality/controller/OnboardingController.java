package com.travelbird.personality.controller;

import com.travelbird.personality.dto.response.PersonalityTestCompletedResponse;
import com.travelbird.personality.dto.response.PersonalityTestResponse;
import com.travelbird.personality.dto.request.PersonalityTestSubmissionRequest;
import com.travelbird.personality.dto.request.PersonalityTieBreakerRequest;
import com.travelbird.global.security.SecurityUtils;
import com.travelbird.personality.service.OnboardingService;
import com.travelbird.personality.service.PersonalityTestSubmissionResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding/personality-test")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<PersonalityTestResponse> getPersonalityTest() {
        return ResponseEntity.ok(onboardingService.getPersonalityTest(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/submissions")
    public ResponseEntity<?> submit(@RequestBody(required = false) PersonalityTestSubmissionRequest request) {
        PersonalityTestSubmissionResult result =
                onboardingService.submitPersonalityTest(SecurityUtils.getCurrentUserId(), request);
        return switch (result) {
            case PersonalityTestSubmissionResult.Completed completed -> ResponseEntity.ok(completed.response());
            case PersonalityTestSubmissionResult.TieBreakerRequired tieBreaker ->
                    ResponseEntity.status(HttpStatus.ACCEPTED).body(tieBreaker.response());
        };
    }

    @PostMapping("/submissions/tie-breaker")
    public ResponseEntity<PersonalityTestCompletedResponse> submitTieBreaker(
            @RequestBody(required = false) PersonalityTieBreakerRequest request) {
        return ResponseEntity.ok(onboardingService.submitTieBreaker(SecurityUtils.getCurrentUserId(), request));
    }
}
