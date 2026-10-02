package com.travelbird.post.service;

import com.travelbird.file.api.FileLinkService;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 게시글 삭제. backend-functional-spec-v10.md §3.8.4 — 사용자 관점에서는 완전 삭제지만,
 * 참조 무결성을 위해 행은 tombstone으로 남긴다({@link Post#tombstone()}). Trip 경로 잠금
 * 해제({@code deletedAt} 기준 판정), 포토맵 재집계, 커뮤니티·검색·홈 노출 제외는 전부
 * {@code PUBLISHED && deletedAt IS NULL} 조건으로 매번 다시 계산되는 구조라 이 서비스가
 * 별도로 처리할 게 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostDeleteService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final SavedRouteRepository savedRouteRepository;
    private final TripPostReader tripPostReader;
    private final FileLinkService fileLinkService;
    private final UserReader userReader;

    public void delete(Long userId, Long postId) {
        userReader.validateActiveUser(userId);

        // findById(삭제 여부 무관) — tombstone된 행도 trip_id는 유지되므로 소유권 확인 후
        // 멱등 처리 여부를 판단해야 한다. findByPostIdAndDeletedAtIsNull을 쓰면 이미
        // 삭제된 요청과 존재하지 않는 요청을 구분할 수 없다.
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        TripPostReader.TripPostSnapshot trip;
        try {
            trip = tripPostReader.getTripForPost(post.getTripId());
        } catch (BusinessException e) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        if (!Objects.equals(trip.ownerUserId(), userId)) {
            throw new BusinessException(ErrorCode.POST_ACCESS_DENIED);
        }

        if (post.isDeleted()) {
            return; // 이미 삭제됨 — 작성자 본인의 재요청은 멱등 204(§3.8.4).
        }

        List<Long> imageFileIds = postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(postId).stream()
                .map(image -> image.getId().getFileId())
                .toList();

        postImageRepository.deleteByIdPostId(postId);
        postPlaceRepository.deleteByIdPostId(postId);
        postHashtagRepository.deleteByIdPostId(postId);

        post.tombstone();
        // 즉시 flush — dirty-checking에만 맡기면(saveAndFlush 생략) 같은 트랜잭션 안에서
        // 뒤이어 이 Trip에 새 Post를 만들 때 Hibernate가 INSERT를 UPDATE보다 먼저 내보내는
        // 기본 flush 순서 때문에 uk_posts_active_trip과 충돌할 수 있다(PostCreateService의
        // saveAndFlush와 동일한 이유).
        postRepository.saveAndFlush(post);

        // 타 사용자의 저장 관계는 유지하되 접근 불가로 표시한다 — 삭제하지 않는다(§3.8.4).
        savedRouteRepository.markUnavailableBySource(SavedRouteSourceType.POST, postId);

        if (!imageFileIds.isEmpty()) {
            fileLinkService.deleteOwnedFiles(userId, imageFileIds);
        }
    }
}
