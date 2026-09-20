package com.travelbird.savedroute.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import com.travelbird.social.api.SocialRelationReader;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 경로 저장·취소. backend-functional-spec-v10.md §3.10.1.
 *
 * <p>차단 관계인 작성자의 게시글은 명세상 "비공개·차단·삭제 Post는 저장할 수 없다"에
 * 해당해서 {@code POST_NOT_FOUND}로 응답한다 — {@code PostViewService}의 차단 처리(집계만
 * 건너뛰고 404는 아님)와 다르게, 저장은 존재 자체를 숨기는 unified 404 패턴을 그대로
 * 따른다. 이 해석이 맞는지는 PR 리뷰 요청에 명시한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostRouteSaveService {

    private static final List<PostVisibility> SAVABLE_VISIBILITY =
            List.of(PostVisibility.PUBLIC, PostVisibility.MEMO_PRIVATE);

    private final PostRepository postRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final TripPostReader tripPostReader;
    private final SocialRelationReader socialRelationReader;

    public void save(Long userId, Long postId) {
        Post post = postRepository.findByPostIdAndStatusAndVisibilityInAndDeletedAtIsNull(
                        postId, PostStatus.PUBLISHED, SAVABLE_VISIBILITY)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        TripPostReader.TripPostSnapshot trip;
        try {
            trip = tripPostReader.getTripForPost(post.getTripId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        if (userId.equals(trip.ownerUserId())) {
            throw new BusinessException(ErrorCode.CANNOT_SAVE_OWN_ROUTE);
        }
        if (socialRelationReader.isBlockedEitherDirection(userId, trip.ownerUserId())) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        savedRouteRepository.findByUserIdAndSourceTypeAndSourceId(userId, SavedRouteSourceType.POST, postId)
                .ifPresentOrElse(
                        existing -> existing.markAvailable(), // 이미 저장돼 있으면 멱등 처리 — saveCount는 다시 올리지 않는다.
                        () -> {
                            savedRouteRepository.save(SavedRoute.of(userId, SavedRouteSourceType.POST, postId));
                            postRepository.incrementSaveCount(postId);
                        });
    }

    /** 중복 취소는 멱등하게 처리한다 — 저장돼 있지 않아도 그냥 204. */
    public void cancel(Long userId, Long postId) {
        long deleted = savedRouteRepository.deleteByUserIdAndSourceTypeAndSourceId(
                userId, SavedRouteSourceType.POST, postId);
        if (deleted > 0) {
            postRepository.decrementSaveCount(postId);
        }
    }
}
