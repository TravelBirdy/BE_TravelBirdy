package com.travelbird.photomap.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.repository.PostPlaceRepository;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 포토맵(§3.11) 집계의 공통 조립기. "사용자 소유의 발행·비삭제 Post에 실제로 걸린
 * (postId, publishedAt, placeId)" 원시 레코드를 만든다. {@code post_places}는
 * {@code (postId, tripPlaceId)}만 갖고 {@code placeId} 컬럼이 없어서(V1 baseline),
 * {@code tripPlaceId -> placeId} 해석은 Part2 {@code TripPostReader.getTripPlaceSnapshot}을
 * 트립당 한 번씩 배치 호출해서 만든 전역 map으로 처리한다({@code trip_place_id}는 전역
 * surrogate PK라 트립 간 충돌 없음). {@code PhotoMapService}와 향후 마이페이지 통계
 * 실구현(fast-follow) 양쪽에서 재사용하려고 별도 컴포넌트로 뺐다
 * ({@code CommunityPostCardAssembler}/{@code CommunityBlockFilter}와 동일한 이유).
 */
@Component
@RequiredArgsConstructor
class PhotoMapVisitAssembler {

    private final PostRepository postRepository;
    private final PostPlaceRepository postPlaceRepository;
    private final TripPostReader tripPostReader;

    record VisitedPlaceRecord(Long postId, LocalDateTime publishedAt, Long placeId) {
    }

    List<VisitedPlaceRecord> resolveVisitedRecords(Long userId) {
        List<Long> tripIds = tripPostReader.getTripIdsByUser(userId);
        if (tripIds.isEmpty()) {
            return List.of();
        }

        List<Post> posts = postRepository.findByTripIdInAndStatusAndDeletedAtIsNull(tripIds, PostStatus.PUBLISHED);
        if (posts.isEmpty()) {
            return List.of();
        }

        Map<Long, LocalDateTime> publishedAtByPostId = new HashMap<>();
        List<Long> postIds = new ArrayList<>();
        List<Long> distinctTripIds = new ArrayList<>();
        for (Post post : posts) {
            publishedAtByPostId.put(post.getPostId(), post.getPublishedAt());
            postIds.add(post.getPostId());
            if (!distinctTripIds.contains(post.getTripId())) {
                distinctTripIds.add(post.getTripId());
            }
        }

        Map<Long, Long> placeIdByTripPlaceId = new HashMap<>();
        for (Long tripId : distinctTripIds) {
            try {
                for (TripPostReader.TripPostPlaceSnapshot snapshot : tripPostReader.getTripPlaceSnapshot(tripId)) {
                    placeIdByTripPlaceId.put(snapshot.tripPlaceId(), snapshot.placeId());
                }
            } catch (BusinessException e) {
                // 트립을 못 찾음(TRIP_NOT_FOUND 등) — 이 트립의 tripPlaceId들이 map에 안 들어가서
                // 아래에서 해당 post_places 행이 자연히 스킵된다.
            }
        }

        List<PostPlace> postPlaces = postPlaceRepository.findByIdPostIdIn(postIds);
        List<VisitedPlaceRecord> records = new ArrayList<>();
        for (PostPlace postPlace : postPlaces) {
            Long placeId = placeIdByTripPlaceId.get(postPlace.getId().getTripPlaceId());
            if (placeId == null) {
                continue; // 트립 미해결 또는 orphaned tripPlaceId — 방어적 스킵
            }
            Long postId = postPlace.getId().getPostId();
            records.add(new VisitedPlaceRecord(postId, publishedAtByPostId.get(postId), placeId));
        }
        return records;
    }
}
