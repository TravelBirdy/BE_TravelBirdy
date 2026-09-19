package com.travelbird.file.repository;

import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.file.domain.FileStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FileAssetRepository extends JpaRepository<FileAsset, Long> {

    @Query("select f from FileAsset f where f.user.userId = :userId")
    List<FileAsset> findAllByUserId(@Param("userId") Long userId);

    @Query("select count(f) from FileAsset f where f.user.userId = :userId and f.purpose = :purpose "
            + "and f.status in (com.travelbird.file.domain.FileStatus.PENDING, com.travelbird.file.domain.FileStatus.UPLOADED)")
    long countPendingOrUploadedByUserAndPurpose(@Param("userId") Long userId, @Param("purpose") FilePurpose purpose);

    /**
     * 24시간 동안 연결(LINKED)되지 않은 PENDING/UPLOADED 파일 — 배치 삭제 대상(기능명세서 3.5.1).
     */
    List<FileAsset> findAllByStatusInAndExpiresAtBefore(List<FileStatus> statuses, LocalDateTime cutoff);
}
