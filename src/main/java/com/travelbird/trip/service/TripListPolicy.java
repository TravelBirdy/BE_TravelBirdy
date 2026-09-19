package com.travelbird.trip.service;
import com.travelbird.global.error.*; import com.travelbird.trip.entity.TripStatus;
public final class TripListPolicy {private TripListPolicy(){} public static void validate(TripStatus status){if(status==null||status==TripStatus.CANCELLED)throw new BusinessException(ErrorCode.INVALID_REQUEST);}}
