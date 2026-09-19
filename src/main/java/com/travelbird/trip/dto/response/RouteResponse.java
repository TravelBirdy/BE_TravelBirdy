package com.travelbird.trip.dto.response; import java.util.List;
public record RouteResponse(Long tripId,int dayNumber,List<RoutePlace> places,List<Coordinates> routePoints) {}
