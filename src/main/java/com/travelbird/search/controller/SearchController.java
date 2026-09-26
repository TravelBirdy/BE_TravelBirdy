package com.travelbird.search.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.search.controller.dto.SearchResponse;
import com.travelbird.search.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/api/search")
    public SearchResponse search(@RequestParam(required = false) String query,
                                  @RequestParam(required = false) Integer limitPerType,
                                  @RequestParam(required = false) List<String> sigunguCodes) {
        return searchService.search(query, limitPerType, sigunguCodes, SecurityUtils.getCurrentUserId());
    }
}
