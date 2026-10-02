package com.travelbird.place.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.place.controller.dto.PlaceDetailResponse;
import com.travelbird.place.controller.dto.PlaceSearchResponse;
import com.travelbird.place.controller.dto.ResolvePlaceRequest;
import com.travelbird.place.controller.dto.ResolvePlaceResponse;
import com.travelbird.place.service.PlaceDetailService;
import com.travelbird.place.service.PlaceResolveService;
import com.travelbird.place.service.PlaceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceDetailService placeDetailService;
    private final PlaceSearchService placeSearchService;
    private final PlaceResolveService placeResolveService;

    /** {@code /api/places/{placeId}}보다 리터럴 경로가 우선한다. 검색어 검증은 서비스에서 한다. */
    @GetMapping("/api/places/search")
    public PlaceSearchResponse search(@RequestParam(required = false) String query,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) BigDecimal latitude,
                                       @RequestParam(required = false) BigDecimal longitude,
                                       @RequestParam(required = false) Integer start,
                                       @RequestParam(required = false) Integer display) {
        Long viewerId = SecurityUtils.getCurrentUserId();
        return placeSearchService.searchPlaces(query, category, latitude, longitude, start, display, viewerId);
    }

    @PostMapping("/api/places/resolve")
    public ResolvePlaceResponse resolve(@RequestBody ResolvePlaceRequest request) {
        SecurityUtils.getCurrentUserId();
        return placeResolveService.resolve(request);
    }

    @GetMapping("/api/places/{placeId}")
    public PlaceDetailResponse getPlace(@PathVariable Long placeId) {
        Long viewerId = SecurityUtils.getCurrentUserId();
        return placeDetailService.getPlaceDetail(placeId, viewerId);
    }
}
