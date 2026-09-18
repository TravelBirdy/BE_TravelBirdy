package com.travelbird.ai.dto.internal;
import com.travelbird.place.entity.PlaceCategory;import java.math.BigDecimal;import java.util.List;
public record PlaceSyncRequest(List<PlaceSyncItem> places){public record PlaceSyncItem(Long placeId,String regionCode,String name,String address,BigDecimal latitude,BigDecimal longitude,PlaceCategory category){}}
