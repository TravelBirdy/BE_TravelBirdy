package com.travelbird.community.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 게시글 신고. Owner = Part 3. backend-functional-spec-v10.md §3.9.6 — 동일 사용자는
 * 동일 게시글을 사유와 무관하게 영구적으로 한 번만 신고할 수 있다
 * ({@code uk_report_user_post}). 관리자 화면/API가 없어 {@code status}는 이 프로젝트
 * 범위에서 항상 {@code RECEIVED}다.
 */
@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long reportId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 30)
    private ReportReasonCode reasonCode;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ReportStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Report(Long reporterUserId, Long postId, ReportReasonCode reasonCode, String description) {
        this.reporterUserId = reporterUserId;
        this.postId = postId;
        this.reasonCode = reasonCode;
        this.description = description;
        this.status = ReportStatus.RECEIVED;
    }

    public static Report create(Long reporterUserId, Long postId, ReportReasonCode reasonCode, String description) {
        return new Report(reporterUserId, postId, reasonCode, description);
    }
}
