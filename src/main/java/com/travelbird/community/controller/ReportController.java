package com.travelbird.community.controller;

import com.travelbird.community.controller.dto.ReportRequest;
import com.travelbird.community.controller.dto.ReportResponse;
import com.travelbird.community.service.ReportService;
import com.travelbird.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 게시글 신고. backend-functional-spec-v10.md §3.9.6 — 로그인 필수. */
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/api/reports")
    public ResponseEntity<ReportResponse> report(@RequestBody ReportRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ReportResponse response = reportService.submit(
                userId, request.reportedPostId(), request.reasonCode(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
