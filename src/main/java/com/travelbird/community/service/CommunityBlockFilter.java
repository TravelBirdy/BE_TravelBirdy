package com.travelbird.community.service;

import com.travelbird.social.api.SocialRelationReader;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 차단 관계 필터링 공용 로직. 커뮤니티 전체/인기/이웃새 목록과 검색이 전부 이 로직을
 * 공유한다. {@code posts}엔 작성자 컬럼이 없어서, 차단(또는 팔로우) 대상 유저들의
 * tripId 목록을 {@code TripPostReader.getTripIdsByUser}로 구해 {@code posts.trip_id}
 * 조건으로 거는 우회 방식을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class CommunityBlockFilter {

    /** JPQL/네이티브/QueryDSL {@code NOT IN}에 빈 컬렉션을 못 써서, 제외할 게 없을 때 넘기는 절대 존재할 수 없는 값. */
    public static final List<Long> NO_EXCLUSION = List.of(-1L);

    private final SocialRelationReader socialRelationReader;
    private final TripPostReader tripPostReader;

    /** {@code viewerIdOrNull}이 차단(양방향)한 유저들의 tripId 목록. 없거나 비로그인이면 {@link #NO_EXCLUSION}. */
    public List<Long> resolveExcludedTripIds(Long viewerIdOrNull) {
        if (viewerIdOrNull == null) {
            return NO_EXCLUSION;
        }
        List<Long> blockedTripIds = resolveTripIdsForUsers(
                socialRelationReader.getBlockedUserIdsEitherDirection(viewerIdOrNull));
        return blockedTripIds.isEmpty() ? NO_EXCLUSION : blockedTripIds;
    }

    /**
     * {@code userIds}(차단 또는 팔로우 대상) 각각에 대해 {@code getTripIdsByUser}를 호출해
     * 합친다 — 배치 조회 메서드가 계약에 없어 유저 수만큼 호출한다(차단/팔로우 목록은
     * 보통 크지 않아 감내 가능한 수준으로 판단).
     */
    public List<Long> resolveTripIdsForUsers(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<Long> tripIds = new ArrayList<>();
        for (Long userId : userIds) {
            tripIds.addAll(tripPostReader.getTripIdsByUser(userId));
        }
        return tripIds;
    }

    public List<Long> getFollowingUserIds(Long viewerId) {
        return socialRelationReader.getFollowingUserIds(viewerId);
    }
}
