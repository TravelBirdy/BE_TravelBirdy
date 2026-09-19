package com.travelbird.social.controller;

import com.travelbird.social.dto.response.BlockedUsersResponse;
import com.travelbird.social.dto.response.FollowListResponse;
import com.travelbird.social.domain.FollowListType;
import com.travelbird.global.security.SecurityUtils;
import com.travelbird.social.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    @PutMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long targetUserId) {
        socialService.follow(SecurityUtils.getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetUserId}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long targetUserId) {
        socialService.unfollow(SecurityUtils.getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/follows")
    public ResponseEntity<FollowListResponse> getFollowList(
            @PathVariable Long userId,
            @RequestParam FollowListType type,
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(socialService.getFollowList(userId, type, cursor, size));
    }

    @PutMapping("/{targetUserId}/block")
    public ResponseEntity<Void> block(@PathVariable Long targetUserId) {
        socialService.block(SecurityUtils.getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetUserId}/block")
    public ResponseEntity<Void> unblock(@PathVariable Long targetUserId) {
        socialService.unblock(SecurityUtils.getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/blocked-users")
    public ResponseEntity<BlockedUsersResponse> getBlockedUsers(
            @RequestParam(required = false) Long cursor,
            @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(socialService.getBlockedUsers(SecurityUtils.getCurrentUserId(), cursor, size));
    }
}
