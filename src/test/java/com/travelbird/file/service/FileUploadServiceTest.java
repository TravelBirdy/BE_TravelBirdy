package com.travelbird.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.file.client.S3FileStorage;
import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.file.dto.request.PresignedUploadRequest;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.user.domain.User;
import com.travelbird.user.domain.UserStatus;
import com.travelbird.user.repository.UserRepository;
import java.awt.Dimension;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceTest {

    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3FileStorage s3FileStorage;
    @Mock
    private WebpImageValidator webpImageValidator;

    private FileUploadService fileUploadService;
    private User activeUser;

    @BeforeEach
    void setUp() {
        fileUploadService = new FileUploadService(
                fileAssetRepository, userRepository, s3FileStorage, webpImageValidator, 15L, 24L);

        activeUser = User.createFromKakao("user@kakao.com");
        ReflectionTestUtils.setField(activeUser, "userId", 1L);
        ReflectionTestUtils.setField(activeUser, "status", UserStatus.ACTIVE);
    }

    private PresignedUploadRequest validRequest() {
        return new PresignedUploadRequest("photo.webp", "image/webp", 400_000L, 1080, 720, FilePurpose.TRIP_PLACE);
    }

    @Test
    void createPresignedUpload_wrongContentType_throwsUnsupportedFileType() {
        PresignedUploadRequest request =
                new PresignedUploadRequest("photo.jpg", "image/jpeg", 400_000L, 1080, 720, FilePurpose.TRIP_PLACE);

        assertThatThrownBy(() -> fileUploadService.createPresignedUpload(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void createPresignedUpload_tooLarge_throwsFileTooLarge() {
        PresignedUploadRequest request =
                new PresignedUploadRequest("photo.webp", "image/webp", 600_000L, 1080, 720, FilePurpose.TRIP_PLACE);

        assertThatThrownBy(() -> fileUploadService.createPresignedUpload(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void createPresignedUpload_widthExceeded_throwsImageDimensionLimitExceeded() {
        PresignedUploadRequest request =
                new PresignedUploadRequest("photo.webp", "image/webp", 400_000L, 1200, 720, FilePurpose.TRIP_PLACE);

        assertThatThrownBy(() -> fileUploadService.createPresignedUpload(1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_DIMENSION_LIMIT_EXCEEDED);
    }

    @Test
    void createPresignedUpload_pendingLimitExceeded_throwsImageLimitExceeded() {
        when(fileAssetRepository.countPendingOrUploadedByUserAndPurpose(1L, FilePurpose.TRIP_PLACE))
                .thenReturn(3L);

        assertThatThrownBy(() -> fileUploadService.createPresignedUpload(1L, validRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.IMAGE_LIMIT_EXCEEDED);
    }

    @Test
    void createPresignedUpload_userNotActive_throwsUserNotActive() {
        when(fileAssetRepository.countPendingOrUploadedByUserAndPurpose(anyLong(), any())).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileUploadService.createPresignedUpload(1L, validRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_ACTIVE);
    }

    @Test
    void createPresignedUpload_valid_returnsResponseAndPersistsPendingFile() throws Exception {
        when(fileAssetRepository.countPendingOrUploadedByUserAndPurpose(anyLong(), any())).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(fileAssetRepository.save(any(FileAsset.class))).thenAnswer(invocation -> {
            FileAsset saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "fileId", 100L);
            return saved;
        });
        URL presignedUrl = URI.create("https://s3.example.com/bucket/key?sig=abc").toURL();
        when(s3FileStorage.createPresignedPutUrl(any(), eq("image/webp"), any()))
                .thenReturn(new S3FileStorage.PresignedUpload(presignedUrl, Instant.now()));

        var response = fileUploadService.createPresignedUpload(1L, validRequest());

        assertThat(response.fileId()).isEqualTo(100L);
        assertThat(response.uploadUrl()).isEqualTo(presignedUrl.toString());
        assertThat(response.objectKey()).contains("trip_place").contains("1");
    }

    @Test
    void completeUpload_fileNotFound_throwsFileNotFound() {
        when(fileAssetRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileUploadService.completeUpload(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);
    }

    @Test
    void completeUpload_notOwnedByCaller_throwsFileNotFound() {
        FileAsset fileAsset = pendingFileAsset();
        when(fileAssetRepository.findById(100L)).thenReturn(Optional.of(fileAsset));

        assertThatThrownBy(() -> fileUploadService.completeUpload(999L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_NOT_FOUND);

        verify(s3FileStorage, never()).getObjectBytes(any());
    }

    @Test
    void completeUpload_alreadyUploaded_isIdempotentAndSkipsRevalidation() {
        FileAsset fileAsset = pendingFileAsset();
        fileAsset.markUploaded();
        when(fileAssetRepository.findById(100L)).thenReturn(Optional.of(fileAsset));

        fileUploadService.completeUpload(1L, 100L);

        verify(s3FileStorage, never()).getObjectBytes(any());
    }

    @Test
    void completeUpload_invalidSignature_throwsFileMetadataMismatchAndDeletesObject() {
        FileAsset fileAsset = pendingFileAsset();
        when(fileAssetRepository.findById(100L)).thenReturn(Optional.of(fileAsset));
        byte[] bytes = {1, 2, 3};
        when(s3FileStorage.getObjectBytes("uploads/trip_place/1/abc.webp")).thenReturn(bytes);
        when(webpImageValidator.hasWebpSignature(bytes)).thenReturn(false);

        assertThatThrownBy(() -> fileUploadService.completeUpload(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_METADATA_MISMATCH);

        verify(s3FileStorage, times(1)).delete("uploads/trip_place/1/abc.webp");
    }

    @Test
    void completeUpload_dimensionMismatch_throwsFileMetadataMismatchAndDeletesObject() {
        FileAsset fileAsset = pendingFileAsset();
        when(fileAssetRepository.findById(100L)).thenReturn(Optional.of(fileAsset));
        byte[] bytes = new byte[400_000];
        when(s3FileStorage.getObjectBytes("uploads/trip_place/1/abc.webp")).thenReturn(bytes);
        when(webpImageValidator.hasWebpSignature(bytes)).thenReturn(true);
        when(webpImageValidator.readDimensions(bytes)).thenReturn(new Dimension(999, 720));

        assertThatThrownBy(() -> fileUploadService.completeUpload(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_METADATA_MISMATCH);

        verify(s3FileStorage, times(1)).delete("uploads/trip_place/1/abc.webp");
    }

    @Test
    void completeUpload_valid_marksFileUploaded() {
        FileAsset fileAsset = pendingFileAsset();
        when(fileAssetRepository.findById(100L)).thenReturn(Optional.of(fileAsset));
        byte[] bytes = new byte[400_000];
        when(s3FileStorage.getObjectBytes("uploads/trip_place/1/abc.webp")).thenReturn(bytes);
        when(webpImageValidator.hasWebpSignature(bytes)).thenReturn(true);
        when(webpImageValidator.readDimensions(bytes)).thenReturn(new Dimension(1080, 720));

        fileUploadService.completeUpload(1L, 100L);

        assertThat(fileAsset.getStatus().name()).isEqualTo("UPLOADED");
        verify(s3FileStorage, never()).delete(any());
    }

    private FileAsset pendingFileAsset() {
        FileAsset fileAsset = FileAsset.createPending(activeUser, "photo.webp",
                "uploads/trip_place/1/abc.webp", "image/webp", 400_000L, 1080, 720,
                FilePurpose.TRIP_PLACE, LocalDateTime.now().plusHours(24));
        ReflectionTestUtils.setField(fileAsset, "fileId", 100L);
        return fileAsset;
    }
}
