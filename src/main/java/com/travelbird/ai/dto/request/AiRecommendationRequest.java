package com.travelbird.ai.dto.request;
import com.travelbird.trip.entity.*;import jakarta.validation.constraints.*;import java.time.LocalDate;import java.util.List;
public record AiRecommendationRequest(@NotBlank @Pattern(regexp="^[0-9]{5}$") String regionCode,@NotNull LocalDate startDate,@NotNull LocalDate endDate,@NotNull CompanionType companionType,@NotEmpty @Size(max=3) List<TravelTheme> themes,@NotNull Pace pace,@NotNull RequestType requestType,List<@NotNull Long> savedPlaceIds){public enum RequestType{GENERAL,SAVED_PLACES}}
