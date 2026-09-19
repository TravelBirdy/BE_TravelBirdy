package com.travelbird.trip.dto.response; import com.travelbird.trip.entity.TripStatus; import java.time.LocalDate;
public record TravelCalendarItem(Long tripId,String title,LocalDate startDate,LocalDate endDate,TripStatus status) {}
