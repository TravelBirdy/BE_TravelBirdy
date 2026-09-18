package com.travelbird.trip.dto.response; import java.util.List;
public record WishlistPlaceListResponse(List<WishlistPlaceItem> items,Long nextCursor) {}
