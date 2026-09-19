package com.travelbird.trip.dto.response;
import java.util.List;
public record TripDayResponse(int dayNumber,List<TripPlaceResponse> places) {}
