package com.travelbird.trip.dto.response; import java.util.List;
public record TripListResponse(List<TripListItem> items,Long nextCursor) {}
