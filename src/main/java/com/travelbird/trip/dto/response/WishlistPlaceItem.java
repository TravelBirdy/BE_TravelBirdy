package com.travelbird.trip.dto.response;
import com.travelbird.place.entity.PlaceCategory; import java.time.LocalDateTime;
public record WishlistPlaceItem(Long placeId,String name,PlaceCategory category,String address,String thumbnailUrl,LocalDateTime addedAt,String description) {}
