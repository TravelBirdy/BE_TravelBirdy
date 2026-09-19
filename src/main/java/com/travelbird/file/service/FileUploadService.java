package com.travelbird.file.service;

import com.travelbird.file.client.S3FileStorage;
import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.file.domain.FileStatus;
import com.travelbird.file.dto.request.PresignedUploadRequest;
import com.travelbird.file.dto.response.PresignedUploadResponse;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.user.repository.UserRepository;
import java.awt.Dimension;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이미지 업로드 Presigned URL 발급 + 업로드 완료 확인. 기능명세서 3.5.1.
 *
 * <p>목적별 첨부 개수 제한("장소별 최대 3장, 게시글 전체 최대 10장")은 발급 요청에 대상
 * placeId/postId가 없어 특정 대상 기준으로는 셀 수 없다 — 대신 이 유저가 아직 연결(LINKED)하지
 * 않은(PENDING/UPLOADED) 파일 수를 목적별로 세어 제한한다. 실제 "이 장소/게시글에 몇 장
 * 연결됐는지"는 연결 시점에 소비 Part({@code FileLinkService.validateLinkableFiles})가 확인한다.
 */
@Service
@Transactional
public class FileUploadService {

    private static final String REQUIRED_CONTENT_TYPE = "image/webp";
    private static final long MAX_SIZE_BYTES = 512_000L;
    private static final int MAX_WIDTH_PX = 1080;
    private static final int TRIP_PLACE_PENDING_LIMIT = 3;
    private static final int POST_PENDING_LIMIT = 10;

    private final FileAssetRepository fileAssetRepository;
    private final UserRepository userRepository;
    private final S3FileStorage s3FileStorage;
    private final WebpImageValidator webpImageValidator;
    private final Duration presignedUrlValidity;
    private final Duration pendingFileExpiry;

    public FileUploadService(
            FileAssetRepository fileAssetRepository,
            UserRepository userRepository,
            S3FileStorage s3FileStorage,
            WebpImageValidator webpImageValidator,
            @Value("${file.presigned-upload.validity-minutes}") long presignedUrlValidityMinutes,
            @Value("${file.pending-file.expiry-hours}") long pendingFileExpiryHours
    ) {
        this.fileAssetRepository = fileAssetRepository;
        this.userRepository = userRepository;
        this.s3FileStorage = s3FileStorage;
        this.webpImageValidator = webpImageValidator;
        this.presignedUrlValidity = Duration.ofMinutes(presignedUrlValidityMinutes);
        this.pendingFileExpiry = Duration.ofHours(pendingFileExpiryHours);
    }

    public PresignedUploadResponse createPresignedUpload(Long userId, PresignedUploadRequest request) {
        if (!REQUIRED_CONTENT_TYPE.equals(request.contentType())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
        if (request.sizeBytes() == null || request.sizeBytes() > MAX_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (request.width() == null || request.width() > MAX_WIDTH_PX) {
            throw new BusinessException(ErrorCode.IMAGE_DIMENSION_LIMIT_EXCEEDED);
        }
        if (request.purpose() == null || request.fileName() == null || request.height() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        long pendingCount = fileAssetRepository.countPendingOrUploadedByUserAndPurpose(userId, request.purpose());
        if (pendingCount >= pendingLimitFor(request.purpose())) {
            throw new BusinessException(ErrorCode.IMAGE_LIMIT_EXCEEDED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_ACTIVE));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.USER_NOT_ACTIVE);
        }

        String objectKey = buildObjectKey(userId, request.purpose());
        LocalDateTime expiresAt = LocalDateTime.now().plus(pendingFileExpiry);

        FileAsset fileAsset = FileAsset.createPending(user, request.fileName(), objectKey, request.contentType(),
                request.sizeBytes(), request.width(), request.height(), request.purpose(), expiresAt);
        fileAssetRepository.save(fileAsset);

        S3FileStorage.PresignedUpload presigned =
                s3FileStorage.createPresignedPutUrl(objectKey, REQUIRED_CONTENT_TYPE, presignedUrlValidity);

        return new PresignedUploadResponse(fileAsset.getFileId(), presigned.url().toString(), objectKey, expiresAt);
    }

    /**
     * 완료 API는 멱등하다 — 이미 UPLOADED/LINKED면 재검증 없이 그대로 끝낸다.
     */
    public void completeUpload(Long userId, Long fileId) {
        FileAsset fileAsset = fileAssetRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FILE_NOT_FOUND));
        if (!fileAsset.isOwnedBy(userId)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        if (fileAsset.getStatus() != FileStatus.PENDING) {
            return;
        }

        byte[] objectBytes = s3FileStorage.getObjectBytes(fileAsset.getObjectKey());

        if (!webpImageValidator.hasWebpSignature(objectBytes)) {
            s3FileStorage.delete(fileAsset.getObjectKey());
            throw new BusinessException(ErrorCode.FILE_METADATA_MISMATCH);
        }

        Dimension dimension = webpImageValidator.readDimensions(objectBytes);
        boolean matches = objectBytes.length == fileAsset.getSizeBytes()
                && dimension.width == fileAsset.getWidth()
                && dimension.height == fileAsset.getHeight();
        if (!matches) {
            s3FileStorage.delete(fileAsset.getObjectKey());
            throw new BusinessException(ErrorCode.FILE_METADATA_MISMATCH);
        }

        fileAsset.markUploaded();
    }

    private int pendingLimitFor(FilePurpose purpose) {
        return purpose == FilePurpose.TRIP_PLACE ? TRIP_PLACE_PENDING_LIMIT : POST_PENDING_LIMIT;
    }

    private String buildObjectKey(Long userId, FilePurpose purpose) {
        return "uploads/%s/%d/%s.webp".formatted(purpose.name().toLowerCase(), userId, UUID.randomUUID());
    }
}
