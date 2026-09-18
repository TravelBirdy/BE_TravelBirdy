package com.travelbird.file.api;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.file.domain.FilePurpose;
import java.util.List;

/**
 * Part 2(Trip 이미지)와 Part 3(Post 이미지)가 파일 연결에 사용하는 공개 계약.
 * 소유권, UPLOADED 상태, purpose(POST|TRIP_PLACE) 검증 책임은 이 구현이 가진다.
 */
public interface FileLinkService {

    void validateLinkableFiles(Long userId, List<Long> fileIds, FilePurpose purpose);

    void markLinked(List<Long> fileIds);

    void deleteOwnedFiles(Long userId, List<Long> fileIds);

    /**
     * 존재하지 않거나 삭제된 fileId는 결과에서 제외한다. 순서는 보장하지 않는다.
     */
    List<ImageSummary> getImageUrls(List<Long> fileIds);
}
