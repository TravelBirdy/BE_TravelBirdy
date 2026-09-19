package com.travelbird.home.controller;

import com.travelbird.home.dto.response.HomeResponse;
import com.travelbird.home.service.HomeService;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/api/home")
    public ResponseEntity<HomeResponse> getHome(
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) BigDecimal longitude
    ) {
        return ResponseEntity.ok(homeService.getHome(latitude, longitude));
    }
}
