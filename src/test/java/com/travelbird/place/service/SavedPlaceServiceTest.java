package com.travelbird.place.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code roundToStoredPrecision}은 MySQL DATETIME(초 단위) 저장 시 반올림 동작을 재현한다.
 * 순수 계산 로직이라 DB/타이밍 없이 결정적으로 검증한다 — 원래 truncate로 구현했다가
 * Testcontainers 통합테스트에서 간헐적으로만 드러나는 버그를 만든 적이 있어(같은 초에 여러
 * 건 저장될 때 커서 페이지가 빈 목록으로 나옴) 회귀 방지용으로 고정해둔다.
 */
class SavedPlaceServiceTest {

    private final SavedPlaceService savedPlaceService = new SavedPlaceService(null, null);

    @Test
    void 반초_이상이면_올림한다() {
        LocalDateTime input = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 600_000_000);

        LocalDateTime result = savedPlaceService.roundToStoredPrecision(input);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 1));
    }

    @Test
    void 반초_미만이면_버린다() {
        LocalDateTime input = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 400_000_000);

        LocalDateTime result = savedPlaceService.roundToStoredPrecision(input);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 0));
    }

    @Test
    void 정확히_반초면_올림한다() {
        LocalDateTime input = LocalDateTime.of(2026, 1, 1, 0, 0, 0, 500_000_000);

        LocalDateTime result = savedPlaceService.roundToStoredPrecision(input);

        assertThat(result).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0, 1));
    }

    @Test
    void 나노초가_없으면_그대로다() {
        LocalDateTime input = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        LocalDateTime result = savedPlaceService.roundToStoredPrecision(input);

        assertThat(result).isEqualTo(input);
    }
}
