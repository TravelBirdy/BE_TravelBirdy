package com.travelbird.trip.dto.response;
import com.travelbird.user.entity.BirdType;
public record AuthorSummary(Long userId,String nickname,BirdType birdType) {}
