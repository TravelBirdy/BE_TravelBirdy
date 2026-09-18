package com.travelbird.place.controller;

import com.travelbird.global.security.SecurityUtils;
import com.travelbird.place.controller.dto.SavePlaceRequest;
import com.travelbird.place.controller.dto.SavedPlaceCursorPageResponse;
import com.travelbird.place.controller.dto.SavedPlaceMemoResponse;
import com.travelbird.place.controller.dto.UpdateSavedPlaceMemoRequest;
import com.travelbird.place.domain.SavedPlace;
import com.travelbird.place.service.SavedPlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SavedPlaceController {

    private final SavedPlaceService savedPlaceService;

    @PutMapping("/api/users/me/saved-places/{placeId}")
    public ResponseEntity<Void> save(@PathVariable Long placeId,
                                      @RequestBody(required = false) SavePlaceRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        String memo = request == null ? null : request.memo();
        savedPlaceService.save(userId, placeId, memo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/users/me/saved-places/{placeId}")
    public ResponseEntity<Void> unsave(@PathVariable Long placeId) {
        Long userId = SecurityUtils.getCurrentUserId();
        savedPlaceService.unsave(userId, placeId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/api/users/me/saved-places/{placeId}/memo")
    public SavedPlaceMemoResponse updateMemo(@PathVariable Long placeId,
                                              @RequestBody UpdateSavedPlaceMemoRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SavedPlace savedPlace = savedPlaceService.updateMemo(userId, placeId, request.memo());
        return new SavedPlaceMemoResponse(placeId, savedPlace.getMemo(), savedPlace.getUpdatedAt());
    }

    @GetMapping("/api/users/me/saved-places")
    public SavedPlaceCursorPageResponse list(@RequestParam(required = false) Long cursor,
                                              @RequestParam(required = false) Integer size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return savedPlaceService.list(userId, cursor, size);
    }
}
