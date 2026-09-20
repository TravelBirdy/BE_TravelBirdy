package com.travelbird.community.service;

import com.travelbird.community.domain.PostViewHistory;
import com.travelbird.community.repository.PostViewHistoryRepository;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.social.api.SocialRelationReader;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 조회수 증가. backend-functional-spec-v10.md §3.9.4 — 로그인 사용자의 게시글
 * 상세 조회를 사용자당 게시글별 24시간에 한 번만 집계한다. 작성자 본인 조회·비로그인
 * 조회는 집계하지 않는다.
 *
 * <p>차단 관계는 "집계하지 않는다"고만 명시돼 있고 404 예외 목록엔 없어서, 접근
 * 불가능한 게시글(BLOCKED/PRIVATE/DRAFT/삭제)은 {@code 404}로 응답하되 차단 관계는
 * 조용히 집계만 건너뛴다(둘을 같은 사유로 묶지 않음) — 이 해석이 맞는지는 PR 리뷰
 * 요청에 명시.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostViewService {

    private static final List<PostVisibility> VIEWABLE_TO_NON_AUTHOR =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);
    private static final int DEDUP_WINDOW_HOURS = 24;

    private final PostRepository postRepository;
    private final PostViewHistoryRepository postViewHistoryRepository;
    private final TripPostReader tripPostReader;
    private final SocialRelationReader socialRelationReader;

    public void recordView(Long postId, Long viewerIdOrNull) {
        if (viewerIdOrNull == null) {
            return; // 비로그인 조회는 집계하지 않는다 — 컨트롤러는 그대로 204를 반환한다.
        }

        Post post = postRepository.findByPostIdAndDeletedAtIsNull(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        TripPostReader.TripPostSnapshot trip;
        try {
            trip = tripPostReader.getTripForPost(post.getTripId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        if (viewerIdOrNull.equals(trip.ownerUserId())) {
            return; // 작성자 본인 조회는 집계하지 않는다.
        }

        boolean accessible = post.getStatus() == PostStatus.PUBLISHED
                && VIEWABLE_TO_NON_AUTHOR.contains(post.getVisibility());
        if (!accessible) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        if (socialRelationReader.isBlockedEitherDirection(viewerIdOrNull, trip.ownerUserId())) {
            return; // 차단 관계 — 집계만 건너뛴다(404 아님).
        }

        LocalDateTime now = LocalDateTime.now();
        boolean viewedRecently = postViewHistoryRepository
                .countByPostIdAndViewedAtBetween(postId, now.minusHours(DEDUP_WINDOW_HOURS), dedupWindowUpperBound(now)) > 0;
        if (viewedRecently) {
            return;
        }

        postViewHistoryRepository.save(PostViewHistory.of(postId, viewerIdOrNull, now));
        postRepository.incrementViewCount(postId);
    }

    /**
     * {@code post_view_histories.viewed_at}은 MySQL {@code DATETIME}(초 단위)이라 저장 시
     * 나노초를 반올림한다(place.service.SavedPlaceService.roundToStoredPrecision과 동일
     * 이슈 — 0.665740초 -> 저장값 +1초). 직전 조회가 반올림으로 소수점 몇 밀리초 차이로
     * "미래"처럼 저장돼 있으면, 그 직후 이 메서드가 만든 {@code now}(반올림 전, 아직
     * DB를 거치지 않은 값)가 그 저장값보다 먼저처럼 보여 중복 판정 범위(BETWEEN)에서
     * 빠질 수 있다 — 실제로 재현해서 확인함(테스트로 고정). 상한을 1초 여유를 둬서
     * 이 경계 사례를 흡수한다(24시간 창에서 1초는 무시할 수 있는 오차).
     */
    private LocalDateTime dedupWindowUpperBound(LocalDateTime now) {
        return now.plusSeconds(1);
    }
}
