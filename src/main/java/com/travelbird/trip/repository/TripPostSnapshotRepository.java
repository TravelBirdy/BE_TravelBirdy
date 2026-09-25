package com.travelbird.trip.repository;

import com.travelbird.trip.api.TripPostReader.TripPostDaySnapshot;
import com.travelbird.trip.api.TripPostReader.TripPostPlaceSnapshot;
import com.travelbird.trip.api.TripPostReader.TripPostSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Current reads deliberately bypass both an older RR snapshot and the JPA identity map. */
@Repository
public class TripPostSnapshotRepository {
  private final JdbcTemplate jdbc;

  public TripPostSnapshotRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

  public Optional<TripPostSnapshot> lockHeader(Long tripId) {
    return jdbc.query("""
        select trip_id, user_id, sigungu_code, companion_type, visibility, cancelled_at
        from trips where trip_id = ? for update
        """, (rs, n) -> new TripPostSnapshot(
            rs.getLong("trip_id"), rs.getLong("user_id"), rs.getString("sigungu_code"),
            rs.getString("companion_type"), java.util.Set.of(), rs.getString("visibility"),
            rs.getObject("cancelled_at", java.time.LocalDateTime.class), List.of()), tripId)
        .stream().findFirst();
  }

  /** Call only after locking the header and checking ownership. */
  public TripPostSnapshot readCurrentContents(TripPostSnapshot header) {
    var themes = new LinkedHashSet<>(jdbc.queryForList(
        "select theme from trip_themes where trip_id = ? for share", String.class, header.tripId()));
    var days = new LinkedHashMap<Integer, LinkedHashMap<Long, PlaceRow>>();
    jdbc.query("""
        select d.day_number, p.trip_place_id, p.place_id, p.visit_order, p.memo, i.file_id
        from trip_days d
        left join trip_places p on p.trip_day_id = d.trip_day_id
        left join trip_place_images i on i.trip_place_id = p.trip_place_id
        where d.trip_id = ?
        order by d.day_number, p.visit_order, i.display_order
        for share
        """, (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
          int day = rs.getInt("day_number");
          var places = days.computeIfAbsent(day, ignored -> new LinkedHashMap<>());
          Long id = rs.getObject("trip_place_id", Long.class);
          if (id != null) {
            var place = places.get(id);
            if (place == null) {
              place = new PlaceRow(id, rs.getLong("place_id"), day,
                  rs.getInt("visit_order"), rs.getString("memo"), new ArrayList<>());
              places.put(id, place);
            }
            Long fileId = rs.getObject("file_id", Long.class);
            if (fileId != null) place.images().add(fileId);
          }
        }, header.tripId());
    return new TripPostSnapshot(header.tripId(), header.ownerUserId(), header.regionCode(),
        header.companionType(), java.util.Set.copyOf(themes), header.visibility(), header.cancelledAt(),
        days.entrySet().stream().map(day -> new TripPostDaySnapshot(day.getKey(),
            day.getValue().values().stream().map(PlaceRow::snapshot).toList())).toList());
  }

  private record PlaceRow(Long id, Long placeId, int day, int order, String memo, List<Long> images) {
    TripPostPlaceSnapshot snapshot() {
      return new TripPostPlaceSnapshot(id,placeId,day,order,memo,List.copyOf(images));
    }
  }
}
