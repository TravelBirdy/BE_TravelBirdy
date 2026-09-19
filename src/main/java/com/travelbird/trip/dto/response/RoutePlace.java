package com.travelbird.trip.dto.response; import java.math.BigDecimal;
public record RoutePlace(Long tripPlaceId,int order,Long placeId,BigDecimal latitude,BigDecimal longitude) {}
