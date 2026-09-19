package com.travelbird.trip.dto.response; import com.travelbird.common.dto.RegionSummary; import com.travelbird.trip.entity.TripStatus; import java.time.LocalDate;
public record TripListItem(Long tripId,String title,LocalDate startDate,LocalDate endDate,TripStatus status,RegionSummary region) {}
