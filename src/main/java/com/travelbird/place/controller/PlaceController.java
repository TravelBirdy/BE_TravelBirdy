package com.travelbird.place.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.place.controller.dto.PlaceDetailResponse;
import com.travelbird.place.service.PlaceDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceDetailService placeDetailService;

    @GetMapping("/api/places/{placeId}")
    public PlaceDetailResponse getPlace(@PathVariable Long placeId) {
        Long viewerId = SecurityUtils.getCurrentUserId();
        return placeDetailService.getPlaceDetail(placeId, viewerId);
    }
}
