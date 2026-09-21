package com.travelbird.post.service;

import com.travelbird.file.api.FileLinkService;
import com.travelbird.file.domain.FilePurpose;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.controller.dto.CreatePostRequest;
import com.travelbird.post.controller.dto.CreatePostResponse;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostHashtag;
import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.repository.PostHashtagRepository;
import com.travelbird.post.repository.PostImageRepository;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.user.api.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 게시글 작성·임시 저장·발행. backend-functional-spec-v10.md §3.8.1.
 *
 * <p>스펙에 명시되지 않아 판단한 부분 — PR 리뷰 요청:
 * <ul>
 *   <li>{@code placeIds}에 해당 트립 소유가 아닌 장소가 오면 {@code 400
 *       POST_PLACE_NOT_IN_TRIP}(신규 코드)로 거부한다 — 스펙에 전용 에러 코드가 없다.</li>
 *   <li>취소된 Trip({@code cancelledAt != null})으로 게시글을 작성하려 하면
 *       Part2의 {@code TRIP_CANCELLED_READ_ONLY}(409)를 재사용해서 막는다 — §3.8.1
 *       예외 목록엔 없지만 "취소된 여행은 읽기 전용"이라는 기존 Part2 규칙과 일관되게
 *       처리했다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostCreateService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final TripPostReader tripPostReader;
    private final FileLinkService fileLinkService;
    private final PostContentValidator contentValidator;
    private final UserReader userReader;

    public CreatePostResponse create(Long userId, CreatePostRequest request) {
        userReader.validateActiveUser(userId); // 탈퇴·정지 유저가 유효 토큰으로 작성하는 것을 막는다(oriole0419 PR#19 리뷰).

        if (request.tripId() == null || request.visibility() == null || request.publish() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        List<Long> imageFileIds = normalize(request.imageFileIds());
        List<Long> placeIds = normalize(request.placeIds());
        List<String> hashtags = normalize(request.hashtags());
        boolean publish = Boolean.TRUE.equals(request.publish());

        if (request.representativeFileId() != null && !imageFileIds.contains(request.representativeFileId())) {
            // imageFileIds에 없는 대표 이미지를 따로 검증·링크하면 실질적으로 10장 제한을
            // 우회할 수 있다(oriole0419 PR#19 리뷰) — 대표 이미지는 첨부 이미지 목록에
            // 포함된 것만 허용한다.
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        contentValidator.validateTitle(request.title());
        contentValidator.validateContent(request.content());
        contentValidator.validateHashtags(hashtags);
        contentValidator.validateImageCount(imageFileIds);
        contentValidator.validateRequiredForPublish(request.title(), request.content(), publish);

        TripPostReader.TripPostSnapshot trip = tripPostReader.getOwnedTripForPost(userId, request.tripId());
        if (trip.cancelledAt() != null) {
            throw new BusinessException(ErrorCode.TRIP_CANCELLED_READ_ONLY);
        }

        if (postRepository.existsByTripIdAndDeletedAtIsNull(request.tripId())) {
            throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS_FOR_TRIP);
        }

        List<Long> tripPlaceIds = resolveTripPlaceIds(trip, placeIds);

        // representativeFileId는 위에서 imageFileIds에 포함된 것만 허용하도록 이미 검증했으므로
        // imageFileIds 하나만 검증·링크하면 대표 이미지도 함께 커버된다.
        fileLinkService.validateLinkableFiles(userId, imageFileIds, FilePurpose.POST);

        Post post = Post.create(request.tripId(), request.title(), request.content(),
                request.representativeFileId(), request.visibility(), publish);
        try {
            postRepository.saveAndFlush(post);
        } catch (DataIntegrityViolationException e) {
            // uk_posts_active_trip 최종 방어 — existsByTripIdAndDeletedAtIsNull과의 사전검사 사이
            // 경쟁 조건으로 동시에 두 요청이 들어온 경우.
            throw new BusinessException(ErrorCode.POST_ALREADY_EXISTS_FOR_TRIP);
        }

        for (int i = 0; i < imageFileIds.size(); i++) {
            postImageRepository.save(PostImage.of(post.getPostId(), imageFileIds.get(i), i));
        }
        for (Long tripPlaceId : tripPlaceIds) {
            postPlaceRepository.save(PostPlace.of(post.getPostId(), tripPlaceId));
        }
        for (String hashtag : hashtags) {
            postHashtagRepository.save(PostHashtag.of(post.getPostId(), hashtag));
        }
        if (!imageFileIds.isEmpty()) {
            fileLinkService.markLinked(imageFileIds);
        }

        return new CreatePostResponse(post.getPostId(), post.getStatus(), post.getVisibility(),
                post.getPublishedAt(), post.getCreatedAt(), post.isRouteLocked());
    }

    private List<Long> resolveTripPlaceIds(TripPostReader.TripPostSnapshot trip, List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> tripPlaceIdByPlaceId = new HashMap<>();
        trip.days().forEach(day -> day.places().forEach(
                place -> tripPlaceIdByPlaceId.put(place.placeId(), place.tripPlaceId())));

        List<Long> tripPlaceIds = new ArrayList<>();
        for (Long placeId : placeIds) {
            Long tripPlaceId = tripPlaceIdByPlaceId.get(placeId);
            if (tripPlaceId == null) {
                throw new BusinessException(ErrorCode.POST_PLACE_NOT_IN_TRIP);
            }
            tripPlaceIds.add(tripPlaceId);
        }
        return tripPlaceIds;
    }

    private <T> List<T> normalize(List<T> list) {
        return list == null ? List.of() : list.stream().filter(Objects::nonNull).toList();
    }
}
