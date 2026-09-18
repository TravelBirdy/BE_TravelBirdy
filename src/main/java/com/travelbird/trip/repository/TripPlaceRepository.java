package com.travelbird.trip.repository;
import com.travelbird.trip.entity.TripPlace; import java.util.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface TripPlaceRepository extends JpaRepository<TripPlace,Long> {
 Optional<TripPlace> findByIdAndTripId(Long id,Long tripId);
 List<TripPlace> findAllByTripIdAndDayDayNumberOrderByVisitOrder(Long tripId,int dayNumber);
 @Modifying @Query("update TripPlace p set p.visitOrder=p.visitOrder+1000 where p.trip.id=:tripId and p.day.dayNumber=:day") int stageOrders(@Param("tripId") Long tripId,@Param("day") int day);
 @Modifying @Query("update TripPlace p set p.visitOrder=:order where p.id=:id and p.trip.id=:tripId and p.day.dayNumber=:day") int setOrder(@Param("tripId") Long tripId,@Param("day") int day,@Param("id") Long id,@Param("order") int order);
}