package com.travelbird.trip.repository;
import com.travelbird.trip.entity.Trip;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface TripRepository extends JpaRepository<Trip,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from Trip t where t.id=:id") Optional<Trip> findByIdForUpdate(@Param("id") Long id);
 @Query(value="select exists(select 1 from posts p where p.trip_id=:tripId and p.published_at is not null and p.deleted_at is null)",nativeQuery=true) boolean existsPublishedPost(@Param("tripId") Long tripId);
 @Query(value="select p.post_id from posts p where p.trip_id=:tripId and p.published_at is not null and p.deleted_at is null limit 1",nativeQuery=true) Optional<Long> findPublishedPostId(@Param("tripId") Long tripId);
}
