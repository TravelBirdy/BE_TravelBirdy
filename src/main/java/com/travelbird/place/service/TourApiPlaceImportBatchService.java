package com.travelbird.place.service;

import com.travelbird.global.error.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * TourAPI 대량 Import(수천 건 단위)를 행 단위로 실행한다. {@link TourApiPlaceImportService#importPlaces}
 * 호출 하나당 독립된 트랜잭션이 열리므로(Spring 프록시를 통한 별도 빈 호출), 한 행이 실패해도
 * 이후 행 처리에 영향을 주지 않는다 — 초기 대량 적재(초기 적재 전용 Import Runner)에서만 쓴다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TourApiPlaceImportBatchService {

    private static final String EXCLUDED_REASON = "카테고리 제외 규칙에 매치되어 Import 대상에서 제외됨";

    private final TourApiPlaceImportService tourApiPlaceImportService;

    public TourApiImportReport importAll(List<TourApiPlaceImportRow> rows) {
        List<PlaceExternalIdMapping> mappings = new ArrayList<>();
        List<TourApiImportFailure> failures = new ArrayList<>();

        for (TourApiPlaceImportRow row : rows) {
            try {
                List<PlaceExternalIdMapping> result = tourApiPlaceImportService.importPlaces(List.of(row));
                if (result.isEmpty()) {
                    failures.add(new TourApiImportFailure(row.externalPlaceId(), EXCLUDED_REASON));
                } else {
                    mappings.add(result.get(0));
                }
            } catch (RuntimeException e) {
                log.warn("TourAPI Import 실패: externalPlaceId={}", row.externalPlaceId(), e);
                failures.add(new TourApiImportFailure(row.externalPlaceId(), describeFailure(e)));
            }
        }

        return new TourApiImportReport(rows.size(), mappings.size(), failures.size(), mappings, failures);
    }

    private String describeFailure(RuntimeException e) {
        if (e instanceof BusinessException businessException) {
            return businessException.getErrorCode().name();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
