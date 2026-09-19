package com.travelbird.community.repository;

import com.travelbird.community.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterUserIdAndPostId(Long reporterUserId, Long postId);
}
