package com.travelbird.trip.dto.response;
import com.travelbird.common.dto.ImageSummary;
import java.util.List;
public record TripPlaceResponse(Long tripPlaceId,Long placeId,int order,String memo,boolean memoMasked,List<ImageSummary> images,String reason,Coordinates coordinates) {}

