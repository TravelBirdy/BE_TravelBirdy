package com.travelbird.trip.dto.response;
import com.travelbird.common.enums.BirdType;
public record AuthorSummary(Long userId,String nickname,BirdType birdType) {}
