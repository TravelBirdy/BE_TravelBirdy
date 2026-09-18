package com.travelbird.trip.repository;
import com.travelbird.trip.entity.TripWishlistPlace;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface TripWishlistRepository extends JpaRepository<TripWishlistPlace,TripWishlistPlace.Id> {
 boolean existsByTripIdAndPlaceId(Long tripId,Long placeId);
 void deleteByTripIdAndPlaceId(Long tripId,Long placeId);
 @Query("select w from TripWishlistPlace w join fetch w.place where w.trip.id=:tripId and (:cursor is null or w.place.id<:cursor) order by w.place.id desc") List<TripWishlistPlace> findPage(@Param("tripId") Long tripId,@Param("cursor") Long cursor,org.springframework.data.domain.Pageable pageable);
}
