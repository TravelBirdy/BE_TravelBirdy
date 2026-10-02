package com.travelbird.community.repository;

import com.travelbird.community.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterUserIdAndPostId(Long reporterUserId, Long postId);

    /** 회원 탈퇴 시 본인이 접수한 신고 정리. */
    void deleteByReporterUserId(Long reporterUserId);
}
