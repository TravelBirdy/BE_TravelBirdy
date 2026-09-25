package com.travelbird.photomap.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.photomap.controller.dto.PhotoMapPlaceItem;
import com.travelbird.photomap.controller.dto.PhotoMapPlaceListResponse;
import com.travelbird.photomap.controller.dto.PhotoMapRegionItem;
import com.travelbird.photomap.controller.dto.PhotoMapRegionsResponse;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.region.api.RegionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전국 포토맵/지역별 방문 장소 조회. backend-functional-spec-v10.md §3.11. 항상
 * {@code SecurityUtils.getCurrentUserId()} 본인 기준이라 visibility 필터가 없다 —
 * PUBLISHED·비삭제 Post면 PRIVATE이어도 포함한다(Community/SavedRoute와 다른 지점).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoMapService {

    private static final RegionSummary UNKNOWN_REGION = new RegionSummary("00000", "알수없음");

    private final PhotoMapVisitAssembler visitAssembler;
    private final PlaceReader placeReader;
    private final RegionReader regionReader;

    public PhotoMapRegionsResponse getRegions(Long userId) {
        List<PhotoMapVisitAssembler.VisitedPlaceRecord> records = visitAssembler.resolveVisitedRecords(userId);
        if (records.isEmpty()) {
            return new PhotoMapRegionsResponse(List.of(), 0);
        }

        Map<Long, PlaceContract> placesById = resolvePlaces(records);
        Map<String, List<PhotoMapVisitAssembler.VisitedPlaceRecord>> recordsBySigunguCode =
                groupBySigunguCode(records, placesById);

        Map<String, RegionSummary> regionsByCode = resolveRegions(recordsBySigunguCode.keySet());
        List<PhotoMapRegionItem> items = recordsBySigunguCode.keySet().stream()
                .sorted()
                .map(code -> {
                    List<PhotoMapVisitAssembler.VisitedPlaceRecord> regionRecords = recordsBySigunguCode.get(code);
                    long visitedPlaceCount = regionRecords.stream()
                            .map(PhotoMapVisitAssembler.VisitedPlaceRecord::placeId).distinct().count();
                    long recordCount = regionRecords.size();
                    RegionSummary region = regionsByCode.getOrDefault(code, UNKNOWN_REGION);
                    return new PhotoMapRegionItem(region, visitedPlaceCount, recordCount);
                })
                .toList();

        return new PhotoMapRegionsResponse(items, items.size());
    }

    /**
     * 마이페이지 방문 지역 수(§3.14.1 "포토맵과 동일한 5자리 시군구 기준") — {@link #getRegions}의
     * {@code totalVisitedRegionCount}와 같은 집계({@link #groupBySigunguCode})를 재사용해서 두
     * 화면의 숫자가 어긋나지 않게 한다.
     */
    public long countVisitedRegions(Long userId) {
        List<PhotoMapVisitAssembler.VisitedPlaceRecord> records = visitAssembler.resolveVisitedRecords(userId);
        if (records.isEmpty()) {
            return 0;
        }
        return groupBySigunguCode(records, resolvePlaces(records)).size();
    }

    private Map<String, List<PhotoMapVisitAssembler.VisitedPlaceRecord>> groupBySigunguCode(
            List<PhotoMapVisitAssembler.VisitedPlaceRecord> records, Map<Long, PlaceContract> placesById) {
        Map<String, List<PhotoMapVisitAssembler.VisitedPlaceRecord>> recordsBySigunguCode = new HashMap<>();
        for (PhotoMapVisitAssembler.VisitedPlaceRecord record : records) {
            PlaceContract place = placesById.get(record.placeId());
            if (place == null) {
                continue; // PlaceReader가 못 찾음(방어적 스킵)
            }
            recordsBySigunguCode.computeIfAbsent(place.sigunguCode(), code -> new ArrayList<>()).add(record);
        }
        return recordsBySigunguCode;
    }

    /**
     * OpenAPI {@code GET /api/users/me/photomap/regions/{regionCode}/places}는 200/401만
     * 정의하고 404가 없다 — 이전엔 {@code RegionReader.existsBySigunguCode}로 잘못된
     * regionCode를 404 처리했는데(oriole0419 PR#16 리뷰로 의도한 동작인지 질문받음), 스펙에
     * 맞춰 검증을 없앴다. 존재하지 않는/방문한 적 없는 regionCode는 필터 결과가 자연히
     * 비어 200+빈 배열로 응답한다.
     */
    public PhotoMapPlaceListResponse getPlacesByRegion(Long userId, String regionCode) {
        List<PhotoMapVisitAssembler.VisitedPlaceRecord> records = visitAssembler.resolveVisitedRecords(userId);
        if (records.isEmpty()) {
            return new PhotoMapPlaceListResponse(List.of());
        }

        Map<Long, PlaceContract> placesById = resolvePlaces(records);
        Map<Long, List<PhotoMapVisitAssembler.VisitedPlaceRecord>> recordsByPlaceId = new HashMap<>();
        for (PhotoMapVisitAssembler.VisitedPlaceRecord record : records) {
            PlaceContract place = placesById.get(record.placeId());
            if (place == null || !regionCode.equals(place.sigunguCode())) {
                continue;
            }
            recordsByPlaceId.computeIfAbsent(record.placeId(), id -> new ArrayList<>()).add(record);
        }

        // 그룹마다 대표(최신) 레코드를 먼저 뽑아서, 장소 목록 자체를 "대표가 가장 최근인
        // 장소가 먼저" 순으로 정렬한다(판단, PR 리뷰 요청) — DTO엔 publishedAt이 없어서
        // 매핑 전 단계(정렬된 레코드 리스트)에서 정렬 기준을 정한다.
        List<PhotoMapPlaceItem> items = recordsByPlaceId.entrySet().stream()
                .map(entry -> toSortedRecords(entry.getValue()))
                .sorted(Comparator.comparing(
                                (List<PhotoMapVisitAssembler.VisitedPlaceRecord> sorted) -> sorted.get(0).publishedAt())
                        .thenComparing(sorted -> sorted.get(0).placeId())
                        .reversed())
                .map(sorted -> toPlaceItem(placesById.get(sorted.get(0).placeId()), sorted))
                .toList();

        return new PhotoMapPlaceListResponse(items);
    }

    /** publishedAt desc, postId desc(동시각 tie-break) — 첫 번째가 대표(가장 최근) 레코드. */
    private List<PhotoMapVisitAssembler.VisitedPlaceRecord> toSortedRecords(
            List<PhotoMapVisitAssembler.VisitedPlaceRecord> placeRecords) {
        return placeRecords.stream()
                .sorted(Comparator.comparing(PhotoMapVisitAssembler.VisitedPlaceRecord::publishedAt)
                        .thenComparing(PhotoMapVisitAssembler.VisitedPlaceRecord::postId)
                        .reversed())
                .toList();
    }

    private PhotoMapPlaceItem toPlaceItem(PlaceContract place, List<PhotoMapVisitAssembler.VisitedPlaceRecord> sorted) {
        Long representativePostId = sorted.get(0).postId();
        List<Long> otherPostIds = sorted.stream()
                .skip(1)
                .map(PhotoMapVisitAssembler.VisitedPlaceRecord::postId)
                .toList();
        return new PhotoMapPlaceItem(place.placeId(), place.name(), place.latitude(), place.longitude(),
                sorted.size(), representativePostId, otherPostIds);
    }

    private Map<Long, PlaceContract> resolvePlaces(List<PhotoMapVisitAssembler.VisitedPlaceRecord> records) {
        List<Long> distinctPlaceIds = records.stream()
                .map(PhotoMapVisitAssembler.VisitedPlaceRecord::placeId)
                .distinct()
                .toList();
        Map<Long, PlaceContract> placesById = new HashMap<>();
        for (PlaceContract place : placeReader.getPlaces(distinctPlaceIds, null)) {
            placesById.put(place.placeId(), place);
        }
        return placesById;
    }

    private Map<String, RegionSummary> resolveRegions(Collection<String> sigunguCodes) {
        Map<String, RegionSummary> regionsByCode = new HashMap<>();
        for (RegionSummary region : regionReader.getRegions(List.copyOf(sigunguCodes))) {
            regionsByCode.put(region.sigunguCode(), region);
        }
        return regionsByCode;
    }
}
