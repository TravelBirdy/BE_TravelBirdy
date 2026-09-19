package com.travelbird.ai.client;
import com.travelbird.common.enums.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.travelbird.ai.dto.internal.*;
import com.travelbird.ai.entity.AiRequestType;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.trip.entity.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiRecommendationClientTest {
  @Test
  void sendsInternalKeyAndAcceptsOnly202PendingWithSameJobId() {
    var builder = RestClient.builder().baseUrl("http://ai.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new HttpAiRecommendationClient(builder.build(), "secret");
    server.expect(once(), requestTo("http://ai.test/internal/v1/recommendations"))
        .andExpect(method(HttpMethod.POST)).andExpect(header("X-Internal-AI-Key", "secret"))
        .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
            .body("{\"jobId\":7,\"status\":\"PENDING\",\"acceptedAt\":\"2026-09-10T09:00:00Z\"}"));

    assertThat(client.recommend(request()).jobId()).isEqualTo(7L);
    server.verify();
  }

  @Test
  void mapsRemoteContractFailureAndRejectsInvalidAcceptedBody() {
    var builder = RestClient.builder().baseUrl("http://ai.test");
    var server = MockRestServiceServer.bindTo(builder).build();
    var client = new HttpAiRecommendationClient(builder.build(), "secret");
    server.expect(requestTo("http://ai.test/internal/v1/recommendations"))
        .andRespond(withStatus(HttpStatus.ACCEPTED).contentType(MediaType.APPLICATION_JSON)
            .body("{\"jobId\":8,\"status\":\"PENDING\",\"acceptedAt\":\"2026-09-10T09:00:00Z\"}"));

    assertThatThrownBy(() -> client.recommend(request()))
        .isInstanceOf(AiClientException.class)
        .extracting(e -> ((AiClientException) e).errorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
  }

  private RecommendationJobRequest request() {
    return new RecommendationJobRequest(7L, AiRequestType.GENERAL, "11110",
        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), CompanionType.SOLO,
        List.of(TravelTheme.FOOD), Pace.NORMAL, List.of(), List.of(), List.of(), true);
  }
}

