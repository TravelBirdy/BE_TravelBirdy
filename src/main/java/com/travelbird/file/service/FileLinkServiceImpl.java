package com.travelbird.file.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.file.client.S3FileStorage;
import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.file.domain.FileStatus;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FileLinkService} 실 구현. Part 2(Trip 이미지)/Part 3(Post 이미지)가 이 저장소에
 * merge되면 이 Bean을 통해 파일을 연결한다.
 */
@Service
@Transactional
public class FileLinkServiceImpl implements FileLinkService {

    private final FileAssetRepository fileAssetRepository;
    private final S3FileStorage s3FileStorage;
    private final String publicBaseUrl;

    public FileLinkServiceImpl(FileAssetRepository fileAssetRepository, S3FileStorage s3FileStorage,
                                @Value("${file.public-base-url}") String publicBaseUrl) {
        this.fileAssetRepository = fileAssetRepository;
        this.s3FileStorage = s3FileStorage;
        this.publicBaseUrl = publicBaseUrl;
    }

    /**
     * 소유권, UPLOADED 상태, purpose 일치 여부를 검증한다 — 셋 중 하나라도 안 맞으면
     * {@code 403 FILE_ACCESS_DENIED}다(기능명세서 3.7.6 "파일 소유권, 업로드 완료 상태와
     * 목적을 검증한다" — 세 검증 실패를 하나의 코드로 묶어서 쓴다).
     */
    @Override
    @Transactional(readOnly = true)
    public void validateLinkableFiles(Long userId, List<Long> fileIds, FilePurpose purpose) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileAsset> files = fileAssetRepository.findAllById(fileIds);
        if (files.size() != fileIds.size()) {
            throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
        }
        for (FileAsset file : files) {
            boolean linkable = file.isOwnedBy(userId)
                    && file.getStatus() == FileStatus.UPLOADED
                    && file.getPurpose() == purpose;
            if (!linkable) {
                throw new BusinessException(ErrorCode.FILE_ACCESS_DENIED);
            }
        }
    }

    /**
     * 존재하지 않는 fileId는 조용히 건너뛴다 — 소비 Part는 이 메서드 호출 전에
     * {@link #validateLinkableFiles}로 이미 유효성을 확인했다는 전제다.
     */
    @Override
    public void markLinked(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        fileAssetRepository.findAllById(fileIds).forEach(FileAsset::markLinked);
    }

    /**
     * 소유하지 않은 fileId는 조용히 건너뛴다(타 파트의 잘못된 입력으로 남의 파일이 지워지지
     * 않도록). DB 행과 S3 객체를 함께 삭제한다(기능명세서 3.7.6 "제거된 이미지 파일은 즉시
     * 삭제한다").
     */
    @Override
    public void deleteOwnedFiles(Long userId, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<FileAsset> ownedFiles = fileAssetRepository.findAllById(fileIds).stream()
                .filter(file -> file.isOwnedBy(userId))
                .toList();
        ownedFiles.forEach(file -> s3FileStorage.delete(file.getObjectKey()));
        fileAssetRepository.deleteAll(ownedFiles);
    }

    /**
     * 존재하지 않거나 삭제된 fileId는 결과에서 제외한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ImageSummary> getImageUrls(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileAssetRepository.findAllById(fileIds).stream()
                .map(file -> new ImageSummary(file.getFileId(), toPublicUrl(file.getObjectKey())))
                .toList();
    }

    private String toPublicUrl(String objectKey) {
        return publicBaseUrl + "/" + objectKey;
    }
}
