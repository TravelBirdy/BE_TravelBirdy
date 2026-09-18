package com.travelbird.trip.entity;

import com.travelbird.global.error.*;



import jakarta.persistence.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import lombok.*;

@Entity @Table(name="trips") @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Trip {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="trip_id") private Long id;
  @Column(name="user_id",nullable=false) private Long userId;
  @Enumerated(EnumType.STRING) @Column(name="source_type",nullable=false,length=30) private TripSourceType sourceType;
  @Column(nullable=false) private String title;
  @Column(columnDefinition="TEXT") private String summary;
  @Column(name="sigungu_code",nullable=false,length=5) private String regionCode;
  @Column(name="start_date",nullable=false) private LocalDate startDate;
  @Column(name="end_date",nullable=false) private LocalDate endDate;
  @Enumerated(EnumType.STRING) @Column(name="companion_type",nullable=false,length=30) private CompanionType companionType;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Pace pace;
  @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Visibility visibility;
  @Column(name="cancelled_at") private LocalDateTime cancelledAt;
  @Version @Column(nullable=false) private long version;
  @ElementCollection(targetClass=TravelTheme.class) @CollectionTable(name="trip_themes",joinColumns=@JoinColumn(name="trip_id")) @Enumerated(EnumType.STRING) @Column(name="theme") private Set<TravelTheme> themes=new LinkedHashSet<>();
  @ElementCollection @CollectionTable(name="trip_hashtags",joinColumns=@JoinColumn(name="trip_id")) @Column(name="hashtag",length=10) private Set<String> hashtags=new LinkedHashSet<>();
  @OneToMany(mappedBy="trip",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("dayNumber") private List<TripDay> days=new ArrayList<>();
  public static Trip createManual(Long userId,String regionCode,LocalDate start,LocalDate end,CompanionType companion,Set<TravelTheme> themes,Pace pace,LocalDateTime now){
    if(start==null||end==null)throw new BusinessException(ErrorCode.TRIP_DATE_REQUIRED);if(end.isBefore(start))throw new BusinessException(ErrorCode.INVALID_TRIP_PERIOD);
    if(companion==null)throw new BusinessException(ErrorCode.INVALID_COMPANION_TYPE);if(pace==null)throw new BusinessException(ErrorCode.INVALID_TRIP_PACE);if(themes==null||themes.isEmpty())throw new BusinessException(ErrorCode.THEME_REQUIRED);if(themes.size()>3)throw new BusinessException(ErrorCode.TOO_MANY_THEMES);
    var t=new Trip();t.userId=userId;t.regionCode=regionCode;t.startDate=start;t.endDate=end;t.companionType=companion;t.themes.addAll(themes);t.pace=pace;t.sourceType=TripSourceType.MANUAL;t.visibility=Visibility.PRIVATE;t.title="여행 일정";
    int count=(int)(ChronoUnit.DAYS.between(start,end)+1);for(int i=1;i<=count;i++)t.days.add(new TripDay(t,i));return t;
  }
  public void markAiSource(){sourceType=TripSourceType.AI;}
  public boolean ownedBy(Long userId){return Objects.equals(this.userId,userId);}
  public TripStatus status(LocalDate today){if(cancelledAt!=null)return TripStatus.CANCELLED;if(today.isBefore(startDate))return TripStatus.UPCOMING;if(today.isAfter(endDate))return TripStatus.COMPLETED;return TripStatus.IN_PROGRESS;}
  public void ensureMutable(){if(cancelledAt!=null)throw new BusinessException(ErrorCode.TRIP_CANCELLED_READ_ONLY);}
  public TripPlace addPlace(int dayNumber,Long placeId){return addPlace(dayNumber,placeId,null);} public TripPlace addPlace(int dayNumber,Long placeId,Integer requestedOrder){ensureMutable();if(days.stream().flatMap(d->d.getPlaces().stream()).anyMatch(p->Objects.equals(p.getPlaceId(),placeId)))throw new BusinessException(ErrorCode.PLACE_ALREADY_ADDED);var d=day(dayNumber);if(d.getPlaces().size()>=15)throw new BusinessException(ErrorCode.TRIP_DAY_PLACE_LIMIT_EXCEEDED);int order=requestedOrder==null?d.getPlaces().size()+1:Math.max(1,Math.min(requestedOrder,d.getPlaces().size()+1));for(var p:d.getPlaces())if(p.getVisitOrder()>=order)p.changeOrder(p.getVisitOrder()+1);return d.addPlace(placeId,order);}
  public void addWishlistPlace(Long placeId){ensureMutable();if(days.stream().flatMap(d->d.getPlaces().stream()).anyMatch(p->Objects.equals(p.getPlaceId(),placeId)))throw new BusinessException(ErrorCode.PLACE_ALREADY_ASSIGNED_TO_DAY);}
  public void applyAiPreview(List<com.travelbird.ai.entity.AiPreviewDay> route){ensureMutable();var existing=new HashMap<Long,TripPlace>();days.stream().flatMap(d->d.getPlaces().stream()).forEach(p->existing.put(p.getPlaceId(),p));existing.values().forEach(p->p.changeOrder(p.getVisitOrder()+1000));for(var d:route)for(var pp:d.getPlaces()){var current=existing.remove(pp.getPlaceId());if(current==null){var added=day(d.getDayNumber()).addPlace(pp.getPlaceId(),pp.getVisitOrder());added.setReason(pp.getReason());}else current.relocate(day(d.getDayNumber()),pp.getVisitOrder());}for(var obsolete:existing.values())obsolete.getDay().getPlaces().remove(obsolete);}
  public void cancel(LocalDateTime now){if(cancelledAt==null)cancelledAt=now;}
  public void updateBasic(String title,boolean titlePresent,String summary,boolean summaryPresent,CompanionType companion,boolean companionPresent,Set<TravelTheme> themes,boolean themesPresent,Pace pace,boolean pacePresent,Visibility visibility,boolean visibilityPresent){ensureMutable();if(titlePresent){if(title==null)throw new BusinessException(ErrorCode.INVALID_REQUEST);this.title=title;}if(summaryPresent)this.summary=summary;if(companionPresent){if(companion==null)throw new BusinessException(ErrorCode.INVALID_REQUEST);this.companionType=companion;}if(themesPresent){if(themes==null||themes.isEmpty())throw new BusinessException(ErrorCode.THEME_REQUIRED);if(themes.size()>3)throw new BusinessException(ErrorCode.TOO_MANY_THEMES);this.themes.clear();this.themes.addAll(themes);}if(pacePresent){if(pace==null)throw new BusinessException(ErrorCode.INVALID_REQUEST);this.pace=pace;}if(visibilityPresent){if(visibility==null)throw new BusinessException(ErrorCode.INVALID_REQUEST);this.visibility=visibility;}}
  public void updateRoute(String regionCode,LocalDate start,LocalDate end,boolean confirmRemoval){ensureMutable();if(end.isBefore(start))throw new BusinessException(ErrorCode.INVALID_TRIP_PERIOD);int wanted=(int)(ChronoUnit.DAYS.between(start,end)+1);if(wanted<days.size()&&!confirmRemoval&&days.subList(wanted,days.size()).stream().anyMatch(d->!d.getPlaces().isEmpty()))throw new BusinessException(ErrorCode.TRIP_DAY_REMOVAL_CONFIRMATION_REQUIRED);while(days.size()>wanted)days.remove(days.size()-1);while(days.size()<wanted)days.add(new TripDay(this,days.size()+1));this.regionCode=regionCode;this.startDate=start;this.endDate=end;}
  public void removePlace(int dayNumber,Long tripPlaceId){ensureMutable();var d=day(dayNumber);var p=d.getPlaces().stream().filter(x->Objects.equals(x.getId(),tripPlaceId)).findFirst().orElseThrow(()->new BusinessException(ErrorCode.PLACE_NOT_FOUND));d.remove(p);}
  public void reorder(int dayNumber,List<Long> ids){ensureMutable();var d=day(dayNumber);if(ids==null||ids.size()!=d.getPlaces().size()||new HashSet<>(ids).size()!=ids.size())throw new BusinessException(ErrorCode.INVALID_PLACE_ORDER_REQUEST);var byId=new HashMap<Long,TripPlace>();for(var p:d.getPlaces())byId.put(p.getId(),p);if(!byId.keySet().equals(new HashSet<>(ids)))throw new BusinessException(ErrorCode.TRIP_PLACE_DAY_MISMATCH);for(var p:d.getPlaces())p.changeOrder(p.getVisitOrder()+1000);d.getPlaces().clear();for(int i=0;i<ids.size();i++){var p=byId.get(ids.get(i));p.changeOrder(i+1);d.getPlaces().add(p);}}  public TripDay day(int number){return days.stream().filter(d->d.getDayNumber()==number).findFirst().orElseThrow(()->new BusinessException(ErrorCode.TRIP_DAY_NOT_FOUND));}
}
