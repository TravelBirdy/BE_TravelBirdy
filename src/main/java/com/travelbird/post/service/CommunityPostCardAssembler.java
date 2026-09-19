package com.travelbird.post.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.domain.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link Post} 목록을 {@link CommunityPostCard}로 변환한다. Home 추천({@link HomePostReaderImpl}),
 * 커뮤니티 전체/인기 목록, 커뮤니티 검색이 전부 같은 카드 모양을 쓰므로 이 클래스로 공유한다.
 *
 * <p>{@code region}/{@code companionType}/{@code themes}/{@code author}는 {@code
 * TripPostReader}(Part2, 미merge) 없이는 채울 수 없어 placeholder다 — TODO(B1 풀리면 교체).
 * {@code thumbnailUrl}은 {@code FileLinkService} 실구현으로 채운다.
 */
@Component
@RequiredArgsConstructor
public class CommunityPostCardAssembler {

    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final FileLinkService fileLinkService;

    public List<CommunityPostCard> toCards(List<Post> posts) {
        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(posts);
        return posts.stream().map(post -> toCard(post, thumbnailUrlsByFileId)).toList();
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

    private CommunityPostCard toCard(Post post, Map<Long, String> thumbnailUrlsByFileId) {
        return new CommunityPostCard(
                post.getPostId(),
                thumbnailUrlsByFileId.get(post.getRepresentativeFileId()),
                post.getTitle(),
                new AuthorSummary(null, "", ""), // TODO(B1 풀리면 교체): trips.user_id -> UserReader
                UNKNOWN_REGION, // TODO(B1 풀리면 교체): TripPostReader로 trips.sigungu_code 조회
                "", // TODO(B1 풀리면 교체): trips.companion_type
                List.of(), // TODO(B1 풀리면 교체): trip_themes
                post.getViewCount(),
                post.getSaveCount(),
                post.getShareCount(),
                false // SavedRoute 도메인(Phase5) 미구현 — 항상 false
        );
    }
}
