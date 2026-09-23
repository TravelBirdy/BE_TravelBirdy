package com.travelbird.post.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.controller.dto.MyPostItem;
import com.travelbird.post.controller.dto.MyPostsResponse;
import com.travelbird.post.domain.Post;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 내 게시글 목록. backend-functional-spec-v10.md §3.8.3. 공개범위·상태 필터 없이
 * 본인 소유 트립의 삭제 안 된 게시글 전체(DRAFT/PUBLISHED/BLOCKED)를 반환한다 — 항상
 * 본인 것만 보여주므로 작성자(author) 조회는 필요 없다({@code CommunityPostCardAssembler}/
 * {@code SavedRouteListService}와 다른 지점).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostMyListService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final PostRepository postRepository;
    private final TripPostReader tripPostReader;
    private final RegionReader regionReader;
    private final FileLinkService fileLinkService;

    public MyPostsResponse list(Long userId, Long cursorOrNull, Integer sizeOrNull) {
        List<Long> tripIds = tripPostReader.getTripIdsByUser(userId);
        if (tripIds.isEmpty()) {
            return new MyPostsResponse(List.of(), null);
        }

        int size = clampSize(sizeOrNull);
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Post> page = (cursorOrNull == null)
                ? postRepository.findMyPostsFirstPage(tripIds, pageable)
                : findAfterCursor(tripIds, cursorOrNull, pageable);

        boolean hasNext = page.size() > size;
        List<Post> content = hasNext ? page.subList(0, size) : page;
        Long nextCursor = hasNext ? content.get(content.size() - 1).getPostId() : null;

        return new MyPostsResponse(assembleItems(content), nextCursor);
    }

    private List<Post> findAfterCursor(List<Long> tripIds, Long cursorPostId, Pageable pageable) {
        Post cursorPost = postRepository.findById(cursorPostId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CURSOR));
        if (!tripIds.contains(cursorPost.getTripId())) {
            throw new BusinessException(ErrorCode.INVALID_CURSOR);
        }
        return postRepository.findMyPostsAfterCursor(
                tripIds, roundToStoredPrecision(cursorPost.getCreatedAt()), cursorPostId, pageable);
    }

    private List<MyPostItem> assembleItems(List<Post> posts) {
        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(posts);
        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = resolveTripSnapshots(posts);
        Map<String, RegionSummary> regionsByCode = resolveRegions(tripsByPostId.values());

        return posts.stream()
                .map(post -> {
                    TripPostReader.TripPostSnapshot trip = tripsByPostId.get(post.getPostId());
                    RegionSummary region = regionsByCode.getOrDefault(trip.regionCode(), UNKNOWN_REGION);
                    return new MyPostItem(
                            post.getPostId(),
                            post.getTripId(),
                            post.getTitle(),
                            thumbnailUrlsByFileId.get(post.getRepresentativeFileId()),
                            region,
                            post.getStatus(),
                            post.getVisibility(),
                            post.getCreatedAt());
                })
                .toList();
    }

    private Map<Long, String> resolveThumbnailUrls(List<Post> posts) {
        List<Long> fileIds = posts.stream()
                .map(Post::getRepresentativeFileId)
                .filter(Objects::nonNull)
                .toList();
        Map<Long, String> urlsByFileId = new HashMap<>();
        for (ImageSummary summary : fileLinkService.getImageUrls(fileIds)) {
            urlsByFileId.put(summary.fileId(), summary.imageUrl());
        }
        return urlsByFileId;
    }

    /**
     * {@code posts.trip_id}는 NOT NULL이고 {@code trips.trip_id}를 FK(ON DELETE
     * RESTRICT)로 참조하므로, 정상 데이터에서 Post는 있는데 연결된 Trip만 없는 상황은
     * 생기지 않는다. 예외를 삼켜 조용히 placeholder로 감추지 않고 그대로 전파한다
     * (chun9930 PR#22 리뷰) — Trip 조회가 실패하면 목록 요청 전체가 실패한다.
     */
    private Map<Long, TripPostReader.TripPostSnapshot> resolveTripSnapshots(List<Post> posts) {
        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = new HashMap<>();
        for (Post post : posts) {
            tripsByPostId.put(post.getPostId(), tripPostReader.getTripForPost(post.getTripId()));
        }
        return tripsByPostId;
    }

    private Map<String, RegionSummary> resolveRegions(java.util.Collection<TripPostReader.TripPostSnapshot> trips) {
        List<String> regionCodes = trips.stream()
                .map(TripPostReader.TripPostSnapshot::regionCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (regionCodes.isEmpty()) {
            return Map.of();
        }
        Map<String, RegionSummary> regionsByCode = new HashMap<>();
        for (RegionSummary region : regionReader.getRegions(regionCodes)) {
            regionsByCode.put(region.sigunguCode(), region);
        }
        return regionsByCode;
    }

    /**
     * {@code posts.created_at}은 MySQL {@code DATETIME}(초 단위)이라 저장 시 나노초를
     * 반올림한다({@code CommunityFeedService.roundToStoredPrecision}과 동일 이슈) — 같은
     * 트랜잭션 안에서 만든 Post를 커서로 재사용할 때 반올림 전 값이 남아있으면 DB 저장값과
     * 안 맞을 수 있어 보정한다.
     */
    private LocalDateTime roundToStoredPrecision(LocalDateTime value) {
        LocalDateTime truncated = value.truncatedTo(ChronoUnit.SECONDS);
        return value.getNano() >= 500_000_000 ? truncated.plusSeconds(1) : truncated;
    }

    private int clampSize(Integer sizeOrNull) {
        if (sizeOrNull == null) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(Math.max(sizeOrNull, 1), MAX_PAGE_SIZE);
    }
}
