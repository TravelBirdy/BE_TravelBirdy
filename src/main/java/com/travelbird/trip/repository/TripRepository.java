package com.travelbird.trip.repository;
import com.travelbird.trip.entity.Trip;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface TripRepository extends JpaRepository<Trip,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from Trip t where t.id=:id") Optional<Trip> findByIdForUpdate(@Param("id") Long id);
}
