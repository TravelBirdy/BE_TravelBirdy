package com.travelbird.ai.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AiTripPreviewTest {
  @Test
  void saveAndCancelChangeRetentionWithoutExtendingOnEdit() {
    LocalDateTime created = LocalDateTime.of(2026, 9, 10, 10, 0);
    AiTripPreview preview = AiTripPreview.temporary(null, null, "제목", "설명", created);
    assertThat(preview.getRetentionStatus()).isEqualTo(AiPreviewRetentionStatus.TEMPORARY);
    assertThat(preview.getExpiresAt()).isEqualTo(created.plusHours(24));

    preview.makePermanent(created.plusHours(1));
    assertThat(preview.getRetentionStatus()).isEqualTo(AiPreviewRetentionStatus.PERMANENT);
    assertThat(preview.getExpiresAt()).isNull();

    preview.makeTemporary(created.plusHours(2));
    assertThat(preview.getExpiresAt()).isEqualTo(created.plusHours(26));
  }
}
