package com.travelbird.place.service;

import java.util.List;

/**
 * TourAPI 대량 Import 결과 — AI·관광데이터 팀에 회신할 매핑/실패 목록/집계.
 */
public record TourApiImportReport(
        int totalCount,
        int successCount,
        int failureCount,
        List<PlaceExternalIdMapping> mappings,
        List<TourApiImportFailure> failures
) {
}
