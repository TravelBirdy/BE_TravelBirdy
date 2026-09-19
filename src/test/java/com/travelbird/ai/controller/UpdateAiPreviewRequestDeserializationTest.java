package com.travelbird.ai.controller;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;import com.travelbird.ai.dto.request.UpdateAiTripPreviewRequest;import org.junit.jupiter.api.Test;
class UpdateAiPreviewRequestDeserializationTest {private final ObjectMapper mapper=new ObjectMapper();@Test void distinguishesAbsentFieldFromExplicitNull() throws Exception{var absent=mapper.readValue("{\"version\":0}",UpdateAiTripPreviewRequest.class);var explicitNull=mapper.readValue("{\"version\":0,\"tripTitle\":null}",UpdateAiTripPreviewRequest.class);assertThat(absent.tripTitlePresent()).isFalse();assertThat(explicitNull.tripTitlePresent()).isTrue();assertThat(explicitNull.tripTitle()).isNull();}}
