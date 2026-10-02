package com.travelbird.post.service;

import com.travelbird.file.api.FileLinkService;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.controller.dto.PostDetailResponse;
import com.travelbird.post.controller.dto.UpdatePostRequest;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostHashtag;
import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 게시글 수정(DRAFT→발행 포함). backend-functional-spec-v10.md §3.8.4.
 *
 * <p>로컬 스펙 파일(2026-09-01 동기화본)엔 PATCH 입력에 {@code publish}와 응답에
 * {@code version}이 빠져 있었다 — 팀장(chun9930) 확인 결과 둘 다 실제 계약에 있는 필드라
 * 추가했다: {@code publish=true}로 DRAFT를 최초 발행하며 Trip 잠금·발행 검증·상태 변경을
 * 같은 트랜잭션에서 처리하고(PR#27 {@code lockOwnedTripForPost}), 응답은
 * {@code PostDetailResponse}에 {@code version}을 포함한다.
 *
 * <p>스펙에 명시되지 않아 판단한 부분 — PR 리뷰 요청:
 * <ul>
 *   <li>{@code publish=true}를 이미 {@code PUBLISHED}/{@code BLOCKED}인 Post에 보내면
 *       에러 없이 무시한다 — "{@code false}/미전달은 상태 유지"와 대칭으로 해석했다.</li>
 *   <li>DRAFT→발행과 동시에 {@code placeIds}를 바꾸는 것은 허용한다 — 호출 시점엔 아직
 *       {@code publishedAt==null}이라 경로 잠금 대상이 아니다(발행 직전 최종 경로 확정
 *       시나리오).</li>
 *   <li>{@code imageFileIds} 교체로 현재 대표 이미지가 첨부 목록 밖으로 밀려났는데 이번
 *       요청이 {@code representativeFileId}를 건드리지 않았다면, 에러 대신 자동으로
 *       연결 해제한다(댕글링 참조 방지) — 요청이 {@code representativeFileId}를 명시했는데
 *       새 첨부 목록 밖이면 기존과 동일하게 {@code 400}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostUpdateService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final TripPostReader tripPostReader;
    private final FileLinkService fileLinkService;
    private final PostContentValidator contentValidator;
    private final PostTripPlaceResolver tripPlaceResolver;
    private final PostDetailService postDetailService;
    private final UserReader userReader;
    private final ApplicationEventPublisher eventPublisher;

    public Optional<PostDetailResponse> update(Long userId, Long postId, UpdatePostRequest request) {
        userReader.validateActiveUser(userId);

        if (request.version() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

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
            return Optional.empty(); // 이미 삭제됨 — 멱등 204(§3.8.4).
        }

        if (!Objects.equals(request.version(), post.getVersion())) {
            throw new BusinessException(ErrorCode.POST_MODIFICATION_CONFLICT);
        }

        if ((request.imageFileIdsPresent() && request.imageFileIds() == null)
                || (request.placeIdsPresent() && request.placeIds() == null)
                || (request.hashtagsPresent() && request.hashtags() == null)
                || (request.visibilityPresent() && request.visibility() == null)) {
            // non-nullable 필드에 explicit null — 문서 78번째 줄 전역 규칙.
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        boolean publishRequested = post.getStatus() == PostStatus.DRAFT && Boolean.TRUE.equals(request.publish());
        if (publishRequested) {
            trip = tripPostReader.lockOwnedTripForPost(userId, post.getTripId());
        }

        if (post.isRouteLocked() && request.placeIdsPresent()) {
            throw new BusinessException(ErrorCode.POST_ROUTE_LOCKED_AFTER_PUBLISH);
        }

        if (request.titlePresent()) {
            contentValidator.validateTitle(request.title());
            post.updateTitle(request.title());
        }
        if (request.contentPresent()) {
            contentValidator.validateContent(request.content());
            post.updateContent(request.content());
        }
        if (request.imageFileIdsPresent()) {
            applyImageFileIds(userId, post, request.imageFileIds());
        }
        applyRepresentativeFile(post, request);
        if (request.placeIdsPresent()) {
            applyPlaceIds(post, trip, request.placeIds());
        }
        if (request.hashtagsPresent()) {
            applyHashtags(post, request.hashtags());
        }
        if (request.visibilityPresent()) {
            post.changeVisibility(request.visibility());
        }

        if (publishRequested) {
            post.publish();
        }
        if (post.getStatus() == PostStatus.PUBLISHED) {
            contentValidator.validateRequiredForPublish(post.getTitle(), post.getContent(), true);
        }

        // 이미지·장소·해시태그처럼 자식 테이블만 바뀐 요청도 Post 행을 dirty로 만들어 @Version을
        // 올리고 동시성 충돌을 검사한다(리뷰 반영).
        post.touch();
        postRepository.saveAndFlush(post);

        return Optional.of(postDetailService.getDetail(postId, userId));
    }

    private void applyImageFileIds(Long userId, Post post, List<Long> imageFileIds) {
        contentValidator.validateImageCount(imageFileIds);
        contentValidator.validateNoDuplicateImages(imageFileIds);

        List<Long> currentFileIds = currentImageFileIds(post);
        List<Long> addedFileIds = imageFileIds.stream().filter(id -> !currentFileIds.contains(id)).toList();
        List<Long> removedFileIds = currentFileIds.stream().filter(id -> !imageFileIds.contains(id)).toList();

        if (!addedFileIds.isEmpty()) {
            fileLinkService.validateLinkableFiles(userId, addedFileIds, FilePurpose.POST);
        }

        // 삭제 후 flush 없이 바로 insert하면 Hibernate 기본 flush 순서(insert가 delete보다
        // 먼저 나감)상 같은 (post_id, display_order) 행이 잠깐 공존해 uk_post_image_order
        // 위반이 날 수 있다(PostDeleteService saveAndFlush와 동일한 원인).
        postImageRepository.deleteByIdPostId(post.getPostId());
        postImageRepository.flush();
        for (int i = 0; i < imageFileIds.size(); i++) {
            postImageRepository.save(PostImage.of(post.getPostId(), imageFileIds.get(i), i));
        }

        if (!addedFileIds.isEmpty()) {
            fileLinkService.markLinked(addedFileIds);
        }
        if (!removedFileIds.isEmpty()) {
            eventPublisher.publishEvent(new PostImagesRemovedEvent(userId, removedFileIds));
        }
    }

    /**
     * {@code representativeFileId}가 present면 (갱신된) 첨부 목록 안에 있는지 검증 후
     * 적용한다(없으면 {@code VALIDATION_ERROR}, {@code PostCreateService}와 동일 규칙).
     * present가 아닌데 {@code imageFileIds} 교체로 기존 대표 이미지가 밖으로 밀려났으면
     * 댕글링 참조를 막기 위해 자동으로 연결을 해제한다.
     */
    private void applyRepresentativeFile(Post post, UpdatePostRequest request) {
        List<Long> currentFileIds = currentImageFileIds(post);
        if (request.representativeFileIdPresent()) {
            Long representativeFileId = request.representativeFileId();
            if (representativeFileId != null && !currentFileIds.contains(representativeFileId)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR);
            }
            post.updateRepresentativeFile(representativeFileId);
        } else if (post.getRepresentativeFileId() != null && !currentFileIds.contains(post.getRepresentativeFileId())) {
            post.updateRepresentativeFile(null);
        }
    }

    private List<Long> currentImageFileIds(Post post) {
        return postImageRepository.findByIdPostIdOrderByDisplayOrderAsc(post.getPostId()).stream()
                .map(image -> image.getId().getFileId())
                .toList();
    }

    private void applyPlaceIds(Post post, TripPostReader.TripPostSnapshot trip, List<Long> placeIds) {
        List<Long> tripPlaceIds = tripPlaceResolver.resolve(trip, placeIds);
        postPlaceRepository.deleteByIdPostId(post.getPostId());
        postPlaceRepository.flush();
        for (Long tripPlaceId : tripPlaceIds) {
            postPlaceRepository.save(PostPlace.of(post.getPostId(), tripPlaceId));
        }
    }

    private void applyHashtags(Post post, List<String> hashtags) {
        contentValidator.validateHashtags(hashtags);
        postHashtagRepository.deleteByIdPostId(post.getPostId());
        postHashtagRepository.flush();
        for (String hashtag : hashtags) {
            postHashtagRepository.save(PostHashtag.of(post.getPostId(), hashtag));
        }
    }
}
