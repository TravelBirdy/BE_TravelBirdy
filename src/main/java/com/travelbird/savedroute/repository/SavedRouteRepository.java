package com.travelbird.savedroute.repository;

import com.travelbird.savedroute.domain.SavedRoute;
import com.travelbird.savedroute.domain.SavedRouteSourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SavedRouteRepository extends JpaRepository<SavedRoute, Long> {

    boolean existsByUserIdAndSourceTypeAndSourceId(Long userId, SavedRouteSourceType sourceType, Long sourceId);

    Optional<SavedRoute> findByUserIdAndSourceTypeAndSourceId(Long userId, SavedRouteSourceType sourceType, Long sourceId);

    Optional<SavedRoute> findByUserIdAndSavedRouteId(Long userId, Long savedRouteId);

    long deleteByUserIdAndSourceTypeAndSourceId(Long userId, SavedRouteSourceType sourceType, Long sourceId);

    /** {@code SavedRouteReferenceReaderImpl}가 씀 — sourceId 하나당 소유자는 항상 최대 1명뿐이라 userId 없이 조회한다. */
    boolean existsBySourceTypeAndSourceIdAndSourceAvailableTrue(SavedRouteSourceType sourceType, Long sourceId);

    @Query("select s from SavedRoute s where s.userId = :userId and s.sourceAvailable = true "
            + "order by s.savedAt desc, s.savedRouteId desc")
    List<SavedRoute> findFirstPage(@Param("userId") Long userId, Pageable pageable);

    @Query("select s from SavedRoute s where s.userId = :userId and s.sourceAvailable = true "
            + "and (s.savedAt < :cursorSavedAt or (s.savedAt = :cursorSavedAt and s.savedRouteId < :cursorId)) "
            + "order by s.savedAt desc, s.savedRouteId desc")
    List<SavedRoute> findPageAfterCursor(@Param("userId") Long userId,
                                          @Param("cursorSavedAt") LocalDateTime cursorSavedAt,
                                          @Param("cursorId") Long cursorId,
                                          Pageable pageable);

    /** 목록 조회 시점에 원본이 더 이상 접근 불가능하다고 판정된 행을 self-heal로 반영한다. */
    @Modifying
    @Query("update SavedRoute s set s.sourceAvailable = false where s.savedRouteId in :savedRouteIds")
    int markUnavailable(@Param("savedRouteIds") List<Long> savedRouteIds);

    /**
     * 원본 Post 삭제(§3.8.4) 시 타인의 저장 관계는 유지하되 접근 불가로 표시한다 — 위
     * {@code markUnavailable}(id 목록 기준)와 달리 sourceId 하나로 그 Post를 저장한 모든
     * 사용자 행을 한 번에 처리한다.
     */
    @Modifying
    @Query("update SavedRoute s set s.sourceAvailable = false "
            + "where s.sourceType = :sourceType and s.sourceId = :sourceId and s.sourceAvailable = true")
    int markUnavailableBySource(@Param("sourceType") SavedRouteSourceType sourceType, @Param("sourceId") Long sourceId);

    /** 회원 탈퇴 시 원본 Post 여러 건을 저장한 타인의 관계를 한 번에 접근 불가로 표시한다. */
    @Modifying
    @Query("update SavedRoute s set s.sourceAvailable = false "
            + "where s.sourceType = :sourceType and s.sourceId in :sourceIds and s.sourceAvailable = true")
    int markUnavailableBySourceIds(@Param("sourceType") SavedRouteSourceType sourceType,
                                    @Param("sourceIds") List<Long> sourceIds);

    List<SavedRoute> findByUserId(Long userId);

    /** {@code CommunityPostCardAssembler}가 카드의 {@code savedRoute} 배지를 채울 때 배치로 쓴다. */
    @Query("select s.sourceId from SavedRoute s where s.userId = :userId and s.sourceType = com.travelbird.savedroute.domain.SavedRouteSourceType.POST "
            + "and s.sourceId in :postIds and s.sourceAvailable = true")
    List<Long> findSavedPostIds(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);
}
