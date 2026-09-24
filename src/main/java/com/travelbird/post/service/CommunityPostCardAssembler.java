package com.travelbird.post.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.BusinessException;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.PostSaveStatusReader;
import com.travelbird.post.domain.Post;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import com.travelbird.user.api.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * {@link Post} 목록을 {@link CommunityPostCard}로 변환한다. Home 추천({@link HomePostReaderImpl}),
 * 커뮤니티 전체/인기 목록, 커뮤니티 검색이 전부 같은 카드 모양을 쓰므로 이 클래스로 공유한다.
 *
 * <p>{@code region}/{@code companionType}/{@code themes}/{@code author.userId}는
 * {@code TripPostReader}(Part2, PR#5 merge됨)로 채운다. {@code author.nickname}/
 * {@code birdType}은 {@code UserReader}(PR#15 merge됨)로 채운다 — 배치 조회 메서드가
 * 계약에 없어서 카드에 등장하는 distinct 작성자 수만큼 개별 호출한다(트립 조회와 동일한
 * 이유로 감수, 클래스 상단 참고). {@code thumbnailUrl}은 {@code FileLinkService}
 * 실구현으로 채운다. {@code savedRoute}는 {@code PostSaveStatusReader}(Phase5,
 * SavedRoute 도메인 실구현)로 채운다.
 */
@Component
@RequiredArgsConstructor
public class CommunityPostCardAssembler {

    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final FileLinkService fileLinkService;
    private final TripPostReader tripPostReader;
    private final RegionReader regionReader;
    private final PostSaveStatusReader postSaveStatusReader;
    private final UserReader userReader;

    public List<CommunityPostCard> toCards(List<Post> posts) {
        return toCards(posts, null);
    }

    public List<CommunityPostCard> toCards(List<Post> posts, Long viewerIdOrNull) {
        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(posts);
        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = resolveTripSnapshots(posts);
        Map<String, RegionSummary> regionsByCode = resolveRegions(tripsByPostId.values());
        Map<Long, UserSummary> authorsByUserId = resolveAuthors(tripsByPostId.values());
        Set<Long> savedPostIds = postSaveStatusReader.findSavedPostIds(
                viewerIdOrNull, posts.stream().map(Post::getPostId).toList());
        return posts.stream()
                .map(post -> toCard(post, thumbnailUrlsByFileId, tripsByPostId.get(post.getPostId()), regionsByCode,
                        authorsByUserId, savedPostIds.contains(post.getPostId())))
                .toList();
    }

    private Map<Long, String> resolveThumbnailUrls(List<Post> posts) {
        List<Long> fileIds = posts.stream()
                .map(Post::getRepresentativeFileId)
                .filter(fileId -> fileId != null)
                .toList();
        Map<Long, String> urlsByFileId = new HashMap<>();
        for (ImageSummary summary : fileLinkService.getImageUrls(fileIds)) {
            urlsByFileId.put(summary.fileId(), summary.imageUrl());
        }
        return urlsByFileId;
    }

    /**
     * 게시글마다 {@code getTripForPost} 한 번씩 호출한다(배치 조회 메서드가 계약에 없음).
     * 트립 하나를 못 찾아도(자연 삭제되지 않는 한 발생하지 않아야 하지만) 그 게시글 카드만
     * placeholder로 빠지게 하고 페이지 전체가 깨지지 않게 한다.
     */
    private Map<Long, TripPostReader.TripPostSnapshot> resolveTripSnapshots(List<Post> posts) {
        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = new HashMap<>();
        for (Post post : posts) {
            try {
                tripsByPostId.put(post.getPostId(), tripPostReader.getTripForPost(post.getTripId()));
            } catch (BusinessException e) {
                // TRIP_NOT_FOUND 등 — 이 카드만 UNKNOWN_REGION/빈 값으로 빠진다.
            }
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
     * 트립마다 해석된 {@code ownerUserId}의 distinct 집합만큼만 {@code getUserSummary}를
     * 호출한다. 유저를 못 찾는 경우(자연 삭제되지 않는 한 발생하지 않아야 함)는 그 작성자만
     * placeholder로 빠지게 하고 페이지 전체가 깨지지 않게 한다.
     */
    private Map<Long, UserSummary> resolveAuthors(java.util.Collection<TripPostReader.TripPostSnapshot> trips) {
        List<Long> authorUserIds = trips.stream()
                .map(TripPostReader.TripPostSnapshot::ownerUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserSummary> authorsByUserId = new HashMap<>();
        for (Long authorUserId : authorUserIds) {
            try {
                authorsByUserId.put(authorUserId, userReader.getUserSummary(authorUserId));
            } catch (BusinessException e) {
                // USER_NOT_FOUND 등 — 이 작성자만 placeholder로 빠진다.
            }
        }
        return authorsByUserId;
    }

    private CommunityPostCard toCard(Post post, Map<Long, String> thumbnailUrlsByFileId,
                                      TripPostReader.TripPostSnapshot tripOrNull,
                                      Map<String, RegionSummary> regionsByCode,
                                      Map<Long, UserSummary> authorsByUserId,
                                      boolean savedRoute) {
        RegionSummary region = tripOrNull == null
                ? UNKNOWN_REGION
                : regionsByCode.getOrDefault(tripOrNull.regionCode(), UNKNOWN_REGION);
        String companionType = tripOrNull == null ? "" : tripOrNull.companionType();
        List<String> themes = tripOrNull == null ? List.of() : List.copyOf(tripOrNull.themes());
        Long authorUserId = tripOrNull == null ? null : tripOrNull.ownerUserId();
        UserSummary authorOrNull = authorUserId == null ? null : authorsByUserId.get(authorUserId);
        AuthorSummary author = new AuthorSummary(
                authorUserId,
                authorOrNull == null ? null : authorOrNull.nickname(),
                authorOrNull == null || authorOrNull.birdType() == null ? null : authorOrNull.birdType().name());

        return new CommunityPostCard(
                post.getPostId(),
                thumbnailUrlsByFileId.get(post.getRepresentativeFileId()),
                post.getTitle(),
                author,
                region,
                companionType,
                themes,
                post.getViewCount(),
                post.getSaveCount(),
                post.getShareCount(),
                savedRoute
        );
    }
}
