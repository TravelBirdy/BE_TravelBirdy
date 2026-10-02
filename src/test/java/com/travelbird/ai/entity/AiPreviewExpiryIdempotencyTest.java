package com.travelbird.ai.entity;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
class AiPreviewExpiryIdempotencyTest {
    @Test void repeatedExpiryKeepsFirstExpiryTimeAndTerminalState() {
        var now=LocalDateTime.of(2026,9,28,0,0);
        var job=AiRecommendationJob.queued(91L,now);
        job.start(now);job.succeed(now);
        var preview=AiTripPreview.temporary(job,1L,"title","summary",now);
        preview.addDay(new AiPreviewDay(1));
        preview.expireContent(now.plusDays(1));
        preview.expireContent(now.plusDays(1).plusMinutes(1));
        assertThat(job.getStatus()).isEqualTo(BackendAiJobStatus.EXPIRED);
        assertThat(job.getExpiredAt()).isEqualTo(now.plusDays(1));
        assertThat(preview.getUpdatedAt()).isEqualTo(now.plusDays(1));
        assertThat(preview.getDays()).isEmpty();
    }
}
