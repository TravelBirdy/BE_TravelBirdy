package com.travelbird.trip.dto.request;
import com.travelbird.common.enums.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize; import com.travelbird.trip.entity.*; import java.time.LocalDate; import java.util.List;
public record CreateTripRequest(String regionCode,LocalDate startDate,LocalDate endDate,@JsonDeserialize(using=CompanionTypeDeserializer.class) CompanionType companionType,List<TravelTheme> themes,@JsonDeserialize(using=PaceDeserializer.class) Pace pace) {}
