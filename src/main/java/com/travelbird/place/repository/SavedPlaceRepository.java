package com.travelbird.place.repository;

import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.domain.SavedPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SavedPlaceRepository extends JpaRepository<SavedPlace, SavedPlaceId> {

    @Query("select s.id.placeId from SavedPlace s where s.id.userId = :userId")
    List<Long> findPlaceIdsByUserId(@Param("userId") Long userId);

    long countByIdUserIdAndIdPlaceIdIn(Long userId, Collection<Long> placeIds);
}
