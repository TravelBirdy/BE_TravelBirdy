package com.travelbird.post.service;

import com.travelbird.common.dto.ImageSummary;
import com.travelbird.common.dto.RegionSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.api.CommunityPostCard;
import com.travelbird.post.api.HomePostReader;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code /api/home} 추천 기록. backend-functional-spec-v10.md "추천 기록은 커뮤니티 인기
 * 점수와 랜덤 셔플을 혼합한다" — 인기 점수(조회수×1+저장수×3+공유수×5) 상위 후보군을 뽑아
 * 셔플 후 {@code limit}개를 취한다.
 *
 * <p><b>부분 구현</b>: {@code region}/{@code companionType}/{@code themes}/{@code author}는
 * {@code trips}(Part2) 데이터가 필요한데, 이를 조회할 Part2 계약({@code TripPostReader})이
 * 아직 main에 없다. Part2 테이블을 직접 조회하는 건 공통협의에서 금지된 패턴이라
 * (파트 간 호출의 3원칙 — "타 파트 Repository 직접 접근 금지") 우회하지 않고, 계약이
 * 생길 때까지 아래 필드들은 placeholder로 둔다.
 *
 * <p>{@code thumbnailUrl}은 {@code FileLinkService}(Part1 PR#11, merge됨) 실구현으로
 * 채운다. 나머지 필드(postId/title/viewCount/saveCount/shareCount)도 전부 실제 데이터다.
 */
@Service
@RequiredArgsConstructor
public class HomePostReaderImpl implements HomePostReader {

    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final PostRepository postRepository;
    private final FileLinkService fileLinkService;

    @Override
    public List<CommunityPostCard> getHomeRecommendedPosts(int limit, Long viewerIdOrNull) {
        if (limit <= 0) {
            return List.of();
        }
        List<Post> candidates = postRepository.findHomeCandidates(
                PostStatus.PUBLISHED,
                List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE),
                PageRequest.of(0, Math.max(limit * 5, 50)));

        List<Post> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);
        List<Post> picked = shuffled.stream().limit(limit).toList();

        Map<Long, String> thumbnailUrlsByFileId = resolveThumbnailUrls(picked);
        return picked.stream().map(post -> toCard(post, thumbnailUrlsByFileId)).toList();
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
