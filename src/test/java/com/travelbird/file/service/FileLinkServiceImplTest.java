package com.travelbird.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.file.client.S3FileStorage;
import com.travelbird.file.domain.FileAsset;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.file.repository.FileAssetRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FileLinkServiceImplTest {

    @Mock
    private FileAssetRepository fileAssetRepository;
    @Mock
    private S3FileStorage s3FileStorage;

    private FileLinkServiceImpl fileLinkService;
    private User owner;

    @BeforeEach
    void setUp() {
        fileLinkService = new FileLinkServiceImpl(fileAssetRepository, s3FileStorage, "https://cdn.travelbird.example");
        owner = User.createFromKakao("owner@kakao.com");
        ReflectionTestUtils.setField(owner, "userId", 1L);
    }

    private FileAsset uploadedFile(Long fileId, FilePurpose purpose) {
        FileAsset fileAsset = FileAsset.createPending(owner, "photo.webp", "uploads/trip_place/1/" + fileId + ".webp",
                "image/webp", 400_000L, 1080, 720, purpose, LocalDateTime.now().plusHours(24));
        ReflectionTestUtils.setField(fileAsset, "fileId", fileId);
        fileAsset.markUploaded();
        return fileAsset;
    }

    @Test
    void validateLinkableFiles_emptyList_doesNothing() {
        fileLinkService.validateLinkableFiles(1L, List.of(), FilePurpose.TRIP_PLACE);

        verify(fileAssetRepository, never()).findAllById(anyList());
    }

    @Test
    void validateLinkableFiles_missingFile_throwsFileAccessDenied() {
        when(fileAssetRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(uploadedFile(10L, FilePurpose.TRIP_PLACE)));

        assertThatThrownBy(() -> fileLinkService.validateLinkableFiles(1L, List.of(10L, 20L), FilePurpose.TRIP_PLACE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_ACCESS_DENIED);
    }

    @Test
    void validateLinkableFiles_notOwnedByCaller_throwsFileAccessDenied() {
        FileAsset file = uploadedFile(10L, FilePurpose.TRIP_PLACE);
        when(fileAssetRepository.findAllById(List.of(10L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> fileLinkService.validateLinkableFiles(999L, List.of(10L), FilePurpose.TRIP_PLACE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_ACCESS_DENIED);
    }

    @Test
    void validateLinkableFiles_notUploadedYet_throwsFileAccessDenied() {
        FileAsset pendingFile = FileAsset.createPending(owner, "photo.webp", "uploads/trip_place/1/10.webp",
                "image/webp", 400_000L, 1080, 720, FilePurpose.TRIP_PLACE, LocalDateTime.now().plusHours(24));
        ReflectionTestUtils.setField(pendingFile, "fileId", 10L);
        when(fileAssetRepository.findAllById(List.of(10L))).thenReturn(List.of(pendingFile));

        assertThatThrownBy(() -> fileLinkService.validateLinkableFiles(1L, List.of(10L), FilePurpose.TRIP_PLACE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_ACCESS_DENIED);
    }

    @Test
    void validateLinkableFiles_wrongPurpose_throwsFileAccessDenied() {
        FileAsset file = uploadedFile(10L, FilePurpose.POST);
        when(fileAssetRepository.findAllById(List.of(10L))).thenReturn(List.of(file));

        assertThatThrownBy(() -> fileLinkService.validateLinkableFiles(1L, List.of(10L), FilePurpose.TRIP_PLACE))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FILE_ACCESS_DENIED);
    }

    @Test
    void validateLinkableFiles_allValid_doesNotThrow() {
        FileAsset file = uploadedFile(10L, FilePurpose.TRIP_PLACE);
        when(fileAssetRepository.findAllById(List.of(10L))).thenReturn(List.of(file));

        fileLinkService.validateLinkableFiles(1L, List.of(10L), FilePurpose.TRIP_PLACE);
    }

    @Test
    void markLinked_marksEachFileLinked() {
        FileAsset file1 = uploadedFile(10L, FilePurpose.TRIP_PLACE);
        FileAsset file2 = uploadedFile(20L, FilePurpose.TRIP_PLACE);
        when(fileAssetRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(file1, file2));

        fileLinkService.markLinked(List.of(10L, 20L));

        assertThat(file1.getStatus().name()).isEqualTo("LINKED");
        assertThat(file2.getStatus().name()).isEqualTo("LINKED");
    }

    @Test
    void deleteOwnedFiles_onlyDeletesOwnedOnes() {
        FileAsset owned = uploadedFile(10L, FilePurpose.TRIP_PLACE);
        User stranger = User.createFromKakao("stranger@kakao.com");
        ReflectionTestUtils.setField(stranger, "userId", 999L);
        FileAsset notOwned = FileAsset.createPending(stranger, "other.webp", "uploads/trip_place/999/20.webp",
                "image/webp", 400_000L, 1080, 720, FilePurpose.TRIP_PLACE, LocalDateTime.now().plusHours(24));
        ReflectionTestUtils.setField(notOwned, "fileId", 20L);

        when(fileAssetRepository.findAllById(List.of(10L, 20L))).thenReturn(List.of(owned, notOwned));

        fileLinkService.deleteOwnedFiles(1L, List.of(10L, 20L));

        verify(s3FileStorage, times(1)).delete("uploads/trip_place/1/10.webp");
        verify(s3FileStorage, never()).delete("uploads/trip_place/999/20.webp");
        verify(fileAssetRepository).deleteAll(List.of(owned));
    }

    @Test
    void getImageUrls_buildsPublicUrlFromObjectKey() {
        FileAsset file = uploadedFile(10L, FilePurpose.TRIP_PLACE);
        when(fileAssetRepository.findAllById(List.of(10L))).thenReturn(List.of(file));

        List<ImageSummary> result = fileLinkService.getImageUrls(List.of(10L));

        assertThat(result).containsExactly(
                new ImageSummary(10L, "https://cdn.travelbird.example/uploads/trip_place/1/10.webp"));
    }

    @Test
    void getImageUrls_emptyInput_returnsEmptyList() {
        List<ImageSummary> result = fileLinkService.getImageUrls(List.of());

        assertThat(result).isEmpty();
        verify(fileAssetRepository, never()).findAllById(anyList());
    }
}
