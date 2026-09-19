package com.travelbird.ai.entity;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
class AiTripPreviewRetentionTest {@Test void repeatedPermanentTransitionPreservesFirstSavedAt(){var created=LocalDateTime.of(2026,9,12,10,0);var preview=AiTripPreview.temporary(null,null,"제목","설명",created);preview.makePermanent(created.plusHours(1));preview.makePermanent(created.plusHours(2));assertThat(preview.getSavedAt()).isEqualTo(created.plusHours(1));}}
