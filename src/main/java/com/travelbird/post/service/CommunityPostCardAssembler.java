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
 * {@code birdType}은 여전히 placeholder다 — {@code UserReader} 인터페이스는 merge돼
 * 있지만 실제 구현체가 프로젝트 어디에도 없어서(Part1에 확인 요청함), 지금 의존성으로
 * 주입받으면 예전 {@code FileLinkService}처럼 스프링 컨텍스트 전체가 부팅 실패한다.
 * 실구현 merge되면 주입하고 채운다. {@code thumbnailUrl}은 {@code FileLinkService}
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

    public List<CommunityPostCard> toCards(List<Post> posts) {
        return toCards(posts, null);
    }

    public List<CommunityPostCard> toCards(List<Post> posts, Long viewerIdOrNull) {
        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(posts);
        Map<Long, TripPostReader.TripPostSnapshot> tripsByPostId = resolveTripSnapshots(posts);
        Map<String, RegionSummary> regionsByCode = resolveRegions(tripsByPostId.values());
        Set<Long> savedPostIds = postSaveStatusReader.findSavedPostIds(
                viewerIdOrNull, posts.stream().map(Post::getPostId).toList());
        return posts.stream()
                .map(post -> toCard(post, thumbnailUrlsByFileId, tripsByPostId.get(post.getPostId()), regionsByCode,
                        savedPostIds.contains(post.getPostId())))
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

    private CommunityPostCard toCard(Post post, Map<Long, String> thumbnailUrlsByFileId,
                                      TripPostReader.TripPostSnapshot tripOrNull,
                                      Map<String, RegionSummary> regionsByCode,
                                      boolean savedRoute) {
        RegionSummary region = tripOrNull == null
                ? UNKNOWN_REGION
                : regionsByCode.getOrDefault(tripOrNull.regionCode(), UNKNOWN_REGION);
        String companionType = tripOrNull == null ? "" : tripOrNull.companionType();
        List<String> themes = tripOrNull == null ? List.of() : List.copyOf(tripOrNull.themes());
        Long authorUserId = tripOrNull == null ? null : tripOrNull.ownerUserId();

        return new CommunityPostCard(
                post.getPostId(),
                thumbnailUrlsByFileId.get(post.getRepresentativeFileId()),
                post.getTitle(),
                new AuthorSummary(authorUserId, "", ""), // TODO(UserReader 실구현 merge되면 교체): nickname/birdType
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
