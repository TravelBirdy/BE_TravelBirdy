package com.travelbird.file.service;

import com.travelbird.file.client.S3FileStorage;
import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FileStatus;
import com.travelbird.file.repository.FileAssetRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 24시간 동안 연결(LINKED)되지 않은 PENDING/UPLOADED 파일을 배치로 삭제한다 (기능명세서 3.5.1).
 */
@Component
public class PendingFileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(PendingFileCleanupScheduler.class);
    private static final List<FileStatus> CLEANUP_TARGET_STATUSES = List.of(FileStatus.PENDING, FileStatus.UPLOADED);

    private final FileAssetRepository fileAssetRepository;
    private final S3FileStorage s3FileStorage;

    public PendingFileCleanupScheduler(FileAssetRepository fileAssetRepository, S3FileStorage s3FileStorage) {
        this.fileAssetRepository = fileAssetRepository;
        this.s3FileStorage = s3FileStorage;
    }

    @Scheduled(cron = "${file.pending-file.cleanup-cron}")
    @Transactional
    public void cleanupExpiredPendingFiles() {
        List<FileAsset> expired = fileAssetRepository.findAllByStatusInAndExpiresAtBefore(
                CLEANUP_TARGET_STATUSES, LocalDateTime.now());
        for (FileAsset fileAsset : expired) {
            try {
                s3FileStorage.delete(fileAsset.getObjectKey());
            } catch (RuntimeException e) {
                log.warn("S3 객체 삭제 실패, DB 행만 정리합니다. fileId={}, objectKey={}",
                        fileAsset.getFileId(), fileAsset.getObjectKey(), e);
            }
            fileAssetRepository.delete(fileAsset);
        }
        if (!expired.isEmpty()) {
            log.info("만료된 PENDING/UPLOADED 파일 {}건 정리 완료", expired.size());
        }
    }
}
