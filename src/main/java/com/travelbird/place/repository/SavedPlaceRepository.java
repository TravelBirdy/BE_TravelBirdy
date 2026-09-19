package com.travelbird.place.repository;

import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.domain.SavedPlaceId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, SavedPlaceId> {

    @Query("select s.id.placeId from SavedPlace s where s.id.userId = :userId")
    List<Long> findPlaceIdsByUserId(@Param("userId") Long userId);

    long countByIdUserIdAndIdPlaceIdIn(Long userId, Collection<Long> placeIds);

    @Query("select s from SavedPlace s where s.id.userId = :userId "
            + "order by s.savedAt desc, s.id.placeId desc")
    List<SavedPlace> findFirstPage(@Param("userId") Long userId, Pageable pageable);

    @Query("select s from SavedPlace s where s.id.userId = :userId "
            + "and (s.savedAt < :cursorSavedAt "
            + "or (s.savedAt = :cursorSavedAt and s.id.placeId < :cursorPlaceId)) "
            + "order by s.savedAt desc, s.id.placeId desc")
    List<SavedPlace> findPageAfterCursor(@Param("userId") Long userId,
                                          @Param("cursorSavedAt") LocalDateTime cursorSavedAt,
                                          @Param("cursorPlaceId") Long cursorPlaceId,
                                          Pageable pageable);
}
