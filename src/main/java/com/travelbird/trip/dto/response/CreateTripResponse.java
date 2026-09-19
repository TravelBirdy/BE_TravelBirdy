package com.travelbird.trip.dto.response;
import com.travelbird.common.enums.*;
import com.travelbird.common.dto.RegionSummary; import com.travelbird.trip.entity.*; import java.time.*; import java.util.*;
public record CreateTripResponse(Long tripId,String title,TripSourceType sourceType,TripStatus status,LocalDateTime cancelledAt,Visibility visibility,RegionSummary region,LocalDate startDate,LocalDate endDate,CompanionType companionType,Set<TravelTheme> themes,Pace pace,Set<String> hashtags,List<TripDayResponse> days,boolean routeEditable,String routeLockedReason) {}

