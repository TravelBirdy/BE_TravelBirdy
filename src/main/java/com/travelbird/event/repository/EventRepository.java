package com.travelbird.event.repository;

import com.travelbird.event.domain.Event;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findByTourApiContentId(String tourApiContentId);

    /**
     * endDate &gt;= today 조건 하나로 기능명세서 3.17.1의
     * "status는 UPCOMING/ONGOING만 반환" 규칙을 만족한다 (종료 이벤트만 걸러내면
     * 남는 건 진행중/예정뿐이라 별도 status 컬럼이 필요 없음).
     */
    @Query("select e from Event e where e.endDate >= :today "
            + "and e.startDate <= :to and e.endDate >= :from "
            + "and (:filterBySigungu = false or e.sigunguCode in :sigunguCodes) "
            + "and (:cursor is null or e.eventId > :cursor) "
            + "order by e.eventId asc")
    List<Event> search(
            @Param("today") LocalDate today,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("filterBySigungu") boolean filterBySigungu,
            @Param("sigunguCodes") List<String> sigunguCodes,
            @Param("cursor") Long cursor,
            Pageable pageable);
}
