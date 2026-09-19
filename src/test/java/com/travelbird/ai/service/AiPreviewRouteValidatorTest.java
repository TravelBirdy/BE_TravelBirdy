package com.travelbird.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.travelbird.ai.dto.internal.AiResultDay;
import com.travelbird.ai.dto.internal.AiResultPlace;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AiPreviewRouteValidatorTest {
  private final AiPreviewRouteValidator validator = new AiPreviewRouteValidator();

  @Test
  void rejectsPlaceDuplicatedAcrossDays() {
    var days = List.of(
        new AiResultDay(1, List.of(new AiResultPlace(10L, 1, "첫째 날 동선에 알맞은 추천 장소입니다."))),
        new AiResultDay(2, List.of(new AiResultPlace(10L, 1, "둘째 날 동선에도 알맞은 추천 장소입니다."))));

    assertThatThrownBy(() -> validator.validate(days, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 2), Set.of(10L), Set.of(), true))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AI_RESPONSE);
  }

  @Test
  void rejectsNonContiguousOrderAndWhitespacePaddedReason() {
    var days = List.of(new AiResultDay(1, List.of(
        new AiResultPlace(10L, 2, "          짧음          "))));

    assertThatThrownBy(() -> validator.validate(days, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 1), Set.of(10L), Set.of(), true))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AI_RESPONSE);
  }

  @Test
  void rejectsMissingRequiredPlacesAndAdditionalPlaceWhenDisabled() {
    var days = List.of(new AiResultDay(1, List.of(
        new AiResultPlace(20L, 1, "필수 장소와 다른 추가 추천 장소입니다."))));

    assertThatThrownBy(() -> validator.validate(days, LocalDate.of(2026, 9, 1),
        LocalDate.of(2026, 9, 1), Set.of(10L, 20L), Set.of(10L), false))
        .isInstanceOf(BusinessException.class)
        .extracting("errorCode").isEqualTo(ErrorCode.INVALID_AI_RESPONSE);
  }
}
