package com.travelbird.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiPreviewReconcileTest {

  @Test
  void reusesExistingPreviewPlaceWhenMovingIt() {
    var preview = AiTripPreview.temporary(
        null, null, "제목", "설명", LocalDateTime.of(2026, 9, 12, 10, 0));
    var day1 = new AiPreviewDay(1);
    var original = new AiPreviewPlace(1L, 1, "기존 추천 이유를 그대로 유지합니다.");
    day1.add(original);
    preview.addDay(day1);

    var day2 = new AiPreviewDay(2);
    day2.add(new AiPreviewPlace(1L, 1, "무시될 임시 추천 이유입니다."));

    preview.reconcileRoute(
        List.of(new AiPreviewDay(1), day2), LocalDateTime.of(2026, 9, 12, 11, 0));

    assertThat(preview.getDays().get(1).getPlaces()).containsExactly(original);
    assertThat(original.getReason()).isEqualTo("기존 추천 이유를 그대로 유지합니다.");
  }
}
