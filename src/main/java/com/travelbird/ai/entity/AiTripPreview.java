package com.travelbird.ai.entity;

import com.travelbird.trip.entity.Trip;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;

@Entity @Table(name="ai_trip_previews") @Getter
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class AiTripPreview {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="preview_id") private Long id;
  @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="job_id",nullable=false,unique=true) private AiRecommendationJob job;
  @Column(name="user_id",nullable=false) private Long userId;
  @Column(name="trip_title") private String tripTitle;
  @Column(columnDefinition="TEXT") private String summary;
  @Enumerated(EnumType.STRING) @Column(name="retention_status",nullable=false) private AiPreviewRetentionStatus retentionStatus;
  @Column(name="expires_at") private LocalDateTime expiresAt;
  @Column(name="saved_at") private LocalDateTime savedAt;
  @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="applied_trip_id") private Trip appliedTrip;
  @Column(name="applied_at") private LocalDateTime appliedAt;
  @Version @Column(nullable=false) private long version;
  @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
  @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
  @OneToMany(mappedBy="preview",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("dayNumber") private List<AiPreviewDay> days=new ArrayList<>();
  @ElementCollection @CollectionTable(name="ai_preview_hashtags",joinColumns=@JoinColumn(name="preview_id")) @Column(name="hashtag",length=10) private Set<String> hashtags=new LinkedHashSet<>();
  public static AiTripPreview temporary(AiRecommendationJob job,Long userId,String title,String summary,LocalDateTime now){var p=new AiTripPreview();p.job=job;p.userId=userId;p.tripTitle=title;p.summary=summary;p.retentionStatus=AiPreviewRetentionStatus.TEMPORARY;p.expiresAt=now.plusHours(24);p.createdAt=now;p.updatedAt=now;return p;}
  public boolean ownedBy(Long uid){return Objects.equals(userId,uid);}
  public boolean expired(LocalDateTime now){return retentionStatus==AiPreviewRetentionStatus.TEMPORARY&&expiresAt!=null&&!expiresAt.isAfter(now);}
  public void makePermanent(LocalDateTime now){if(retentionStatus==AiPreviewRetentionStatus.PERMANENT)return;retentionStatus=AiPreviewRetentionStatus.PERMANENT;expiresAt=null;savedAt=now;updatedAt=now;}
  public void makeTemporary(LocalDateTime now){if(retentionStatus==AiPreviewRetentionStatus.TEMPORARY)return;retentionStatus=AiPreviewRetentionStatus.TEMPORARY;expiresAt=now.plusHours(24);savedAt=null;updatedAt=now;}
  public void replace(String title,String summary,List<String> tags,List<AiPreviewDay> route,LocalDateTime now){if(title!=null)tripTitle=title;if(summary!=null)this.summary=summary;if(tags!=null){hashtags.clear();hashtags.addAll(tags);}if(route!=null){days.clear();for(var d:route){d.attach(this);days.add(d);}}updatedAt=now;}
  public void reconcileRoute(List<AiPreviewDay> requested,LocalDateTime now){var existingDays=new HashMap<Integer,AiPreviewDay>();for(var day:days)existingDays.put(day.getDayNumber(),day);var existingPlaces=new HashMap<Long,AiPreviewPlace>();days.stream().flatMap(day->day.getPlaces().stream()).forEach(place->{place.stageOrder();existingPlaces.put(place.getPlace().getId(),place);});var finalDays=new ArrayList<AiPreviewDay>();for(var incoming:requested){var target=existingDays.remove(incoming.getDayNumber());if(target==null){target=new AiPreviewDay(incoming.getDayNumber());target.attach(this);}var wanted=new ArrayList<>(incoming.getPlaces());for(var desired:wanted){var current=existingPlaces.remove(desired.getPlace().getId());if(current==null)target.add(desired);else current.relocate(target,desired.getVisitOrder());}finalDays.add(target);}for(var obsolete:existingPlaces.values())obsolete.getDay().getPlaces().remove(obsolete);days.removeAll(existingDays.values());for(var day:finalDays)if(!days.contains(day))days.add(day);updatedAt=now;}
  public void addDay(AiPreviewDay day){day.attach(this);days.add(day);}
  public void applied(Trip trip,LocalDateTime now){appliedTrip=trip;appliedAt=now;updatedAt=now;}
  public void expireContent(LocalDateTime now){days.clear();hashtags.clear();tripTitle=null;summary=null;expiresAt=now;updatedAt=now;job.expire(now);}
}

