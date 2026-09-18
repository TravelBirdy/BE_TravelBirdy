package com.travelbird.ai.entity;

import com.travelbird.global.error.AiJobFailureCode;
import com.travelbird.trip.entity.CompanionType;
import com.travelbird.trip.entity.Pace;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity @Table(name="ai_recommendation_jobs") @Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class AiRecommendationJob {
 @Id @Column(name="job_id") private Long id;
 @Column(name="user_id",nullable=false) private Long userId;
 @Column(name="target_trip_id") private Long targetTripId;
 @Enumerated(EnumType.STRING) @Column(name="request_type",nullable=false) private AiRequestType requestType;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private BackendAiJobStatus status;
 @Column(name="region_code",nullable=false,length=5) private String regionCode;
 @Column(name="start_date",nullable=false) private LocalDate startDate;
 @Column(name="end_date",nullable=false) private LocalDate endDate;
 @Enumerated(EnumType.STRING) @Column(name="companion_type",nullable=false) private CompanionType companionType;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Pace pace;
 @Column(nullable=false,columnDefinition="json") private String themes;
 @Column(name="saved_place_ids",nullable=false,columnDefinition="json") private String savedPlaceIds;
 @Column(name="wishlist_place_ids",nullable=false,columnDefinition="json") private String wishlistPlaceIds;
 @Column(name="existing_schedule",nullable=false,columnDefinition="json") private String existingSchedule;
 @Column(name="allow_additional_recommendations",nullable=false) private boolean allowAdditionalRecommendations;
 @Column(name="requested_at",nullable=false) private LocalDateTime requestedAt;
 @Column(name="started_at") private LocalDateTime startedAt;
 @Column(name="completed_at") private LocalDateTime completedAt;
 @Column(name="expired_at") private LocalDateTime expiredAt;
 @Column(name="error_code") private String errorCode;
 @Column(name="error_message") private String errorMessage;
 @Column(name="error_retryable") private Boolean errorRetryable;

 public static AiRecommendationJob queued(Long id,Long userId,Long tripId,AiRequestType type,String region,LocalDate start,LocalDate end,CompanionType companion,Pace pace,String themes,String saved,String wishlist,String schedule,boolean additional,LocalDateTime at){var j=queued(id,at);j.userId=userId;j.targetTripId=tripId;j.requestType=type;j.regionCode=region;j.startDate=start;j.endDate=end;j.companionType=companion;j.pace=pace;j.themes=themes;j.savedPlaceIds=saved;j.wishlistPlaceIds=wishlist;j.existingSchedule=schedule;j.allowAdditionalRecommendations=additional;return j;}
 static AiRecommendationJob queued(Long id,LocalDateTime at){var j=new AiRecommendationJob();j.id=id;j.status=BackendAiJobStatus.QUEUED;j.requestedAt=at;return j;}
 public void start(LocalDateTime at){require(BackendAiJobStatus.QUEUED);status=BackendAiJobStatus.PROCESSING;startedAt=at;}
 public LocalDateTime callbackDeadline(int seconds){return startedAt==null?null:startedAt.plusSeconds(seconds);}
 public void succeed(LocalDateTime at){require(BackendAiJobStatus.PROCESSING);status=BackendAiJobStatus.SUCCEEDED;completedAt=at;}
 public void fail(AiJobFailureCode code,String message,boolean retryable,LocalDateTime at){fail(code.name(),message,retryable,at);}
 public void fail(String code,String message,boolean retryable,LocalDateTime at){if(status!=BackendAiJobStatus.QUEUED&&status!=BackendAiJobStatus.PROCESSING)throw new IllegalStateException("Only active AI jobs can fail");status=BackendAiJobStatus.FAILED;errorCode=code;errorMessage=message;errorRetryable=retryable;completedAt=at;}
 public void expire(LocalDateTime at){require(BackendAiJobStatus.SUCCEEDED);status=BackendAiJobStatus.EXPIRED;expiredAt=at;}
 public boolean ownedBy(Long uid){return java.util.Objects.equals(userId,uid);}
 private void require(BackendAiJobStatus expected){if(status!=expected)throw new IllegalStateException("Invalid AI job transition");}
}

