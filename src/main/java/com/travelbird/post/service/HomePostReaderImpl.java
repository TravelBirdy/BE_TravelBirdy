package com.travelbird.post.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.post.api.AuthorSummary;
import com.travelbird.post.api.HomePostCard;
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
import java.util.List;

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
 * <p>{@code thumbnailUrl}도 마찬가지로 지금은 항상 {@code null}이다 —
 * {@code file.api.FileLinkService}는 계약만 merge돼 있고 실제 구현체가 main 어디에도
 * 없어서(Part1 PR#11에만 있음, 미merge), 지금 이 클래스가 그 인터페이스를 의존성으로
 * 주입받으면 스프링 컨텍스트 전체가 부팅 실패한다(실제로 겪은 문제 — Testcontainers
 * 검증 중 발견). 실구현 merge되면 주입하고 채운다. 나머지 필드(postId/title/
 * viewCount/saveCount/shareCount)는 전부 실제 데이터다.
 */
@Service
@RequiredArgsConstructor
public class HomePostReaderImpl implements HomePostReader {

    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final PostRepository postRepository;

    @Override
    public List<HomePostCard> getHomeRecommendedPosts(int limit, Long viewerIdOrNull) {
        if (limit <= 0) {
            return List.of();
        }
        List<Post> candidates = postRepository.findHomeCandidates(
                PostStatus.PUBLISHED,
                List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE),
                PageRequest.of(0, Math.max(limit * 5, 50)));

        List<Post> shuffled = new ArrayList<>(candidates);
        Collections.shuffle(shuffled);

        return shuffled.stream().limit(limit).map(this::toCard).toList();
    }

    private HomePostCard toCard(Post post) {
        return new HomePostCard(
                post.getPostId(),
                post.getTitle(),
                null, // TODO(FileLinkService 실구현 merge되면 교체): representativeFileId -> thumbnailUrl
                UNKNOWN_REGION, // TODO(B1 풀리면 교체): TripPostReader로 trips.sigungu_code 조회
                new AuthorSummary(null, "", ""), // TODO(B1 풀리면 교체): trips.user_id -> UserReader
                "", // TODO(B1 풀리면 교체): trips.companion_type
                List.of(), // TODO(B1 풀리면 교체): trip_themes
                post.getViewCount(),
                post.getSaveCount(),
                post.getShareCount(),
                false // SavedRoute 도메인(Phase5) 미구현 — 항상 false
        );
    }
}
