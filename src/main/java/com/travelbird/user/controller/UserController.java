package com.travelbird.user.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.user.service.WithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final WithdrawalService withdrawalService;

    public UserController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw() {
        withdrawalService.withdraw(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
