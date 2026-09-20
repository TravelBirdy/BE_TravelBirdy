package com.travelbird.community.service;

import com.travelbird.community.controller.dto.ReportResponse;
import com.travelbird.community.domain.Report;
import com.travelbird.community.domain.ReportReasonCode;
import com.travelbird.community.repository.ReportRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 신고. backend-functional-spec-v10.md §3.9.6.
 *
 * <p>"게시글 접근 가능 여부" 검증을 "삭제되지 않음"으로 좁게 해석한다 —
 * BLOCKED/PRIVATE 게시글도 신고 대상이 될 수 있다고 봤다(신고 사유 자체가 그런
 * 게시글을 겨냥할 수 있어서). 상세 조회의 unified 404(비공개 게시글=존재 자체를
 * 숨김)와는 다른 판단이라 PR 리뷰 요청에 명시한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final PostRepository postRepository;
    private final ReportRepository reportRepository;

    public ReportResponse submit(Long reporterUserId, Long postId, ReportReasonCode reasonCode, String description) {
        if (!postRepository.existsByPostIdAndDeletedAtIsNull(postId)) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (reportRepository.existsByReporterUserIdAndPostId(reporterUserId, postId)) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        Report report;
        try {
            report = reportRepository.saveAndFlush(
                    Report.create(reporterUserId, postId, reasonCode, description));
        } catch (DataIntegrityViolationException e) {
            // uk_report_user_post 위반에 대한 이중 방어(경쟁 조건) — Post.uk_posts_active_trip과 동일 패턴.
            throw new BusinessException(ErrorCode.REPORT_ALREADY_SUBMITTED);
        }

        return new ReportResponse(report.getReportId(), report.getStatus().name(), report.getCreatedAt());
    }
}
