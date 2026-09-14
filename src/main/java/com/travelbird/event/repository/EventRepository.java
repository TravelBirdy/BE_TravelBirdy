package com.travelbird.event.repository;

import com.travelbird.event.domain.Event;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    @Query("select e from Event e where e.endDate >= :today and e.startDate <= :monthEnd order by e.startDate asc")
    List<Event> findOngoingOrUpcomingWithinMonth(@Param("today") LocalDate today, @Param("monthEnd") LocalDate monthEnd);
}
