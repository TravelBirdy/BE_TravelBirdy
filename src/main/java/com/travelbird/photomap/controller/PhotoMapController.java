package com.travelbird.photomap.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.photomap.controller.dto.PhotoMapPlaceListResponse;
import com.travelbird.photomap.controller.dto.PhotoMapRegionsResponse;
import com.travelbird.photomap.service.PhotoMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 포토맵 조회. backend-functional-spec-v10.md §3.11. 전부 로그인 필수(본인 기준). */
@RestController
@RequiredArgsConstructor
public class PhotoMapController {

    private final PhotoMapService photoMapService;

    @GetMapping("/api/users/me/photomap/regions")
    public PhotoMapRegionsResponse getRegions() {
        return photoMapService.getRegions(SecurityUtils.getCurrentUserId());
    }

    @GetMapping("/api/users/me/photomap/regions/{regionCode}/places")
    public PhotoMapPlaceListResponse getPlaces(@PathVariable String regionCode) {
        return photoMapService.getPlacesByRegion(SecurityUtils.getCurrentUserId(), regionCode);
    }
}
