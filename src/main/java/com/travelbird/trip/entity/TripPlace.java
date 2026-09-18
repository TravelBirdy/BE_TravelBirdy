package com.travelbird.trip.entity;

import com.travelbird.file.entity.FileMetadata;
import com.travelbird.place.entity.Place;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;
import lombok.*;

@Entity @Table(name="trip_places", uniqueConstraints={@UniqueConstraint(name="uk_trip_place_global",columnNames={"trip_id","place_id"}),@UniqueConstraint(name="uk_trip_day_visit_order",columnNames={"trip_day_id","visit_order"})})
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TripPlace {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="trip_place_id") private Long id;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_id",nullable=false) private Trip trip;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_day_id",nullable=false) private TripDay day;
  @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="place_id",nullable=false) private Place place;
  @Column(name="visit_order",nullable=false) private int visitOrder;
  @Column(length=100) private String memo;
  @Column(length=100) private String reason;
  @OneToMany(mappedBy="tripPlace",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("displayOrder") private List<TripPlaceImage> images=new ArrayList<>();
  @Column(name="created_at",nullable=false,insertable=false,updatable=false) private LocalDateTime createdAt;
  @Column(name="updated_at",nullable=false,insertable=false,updatable=false) private LocalDateTime updatedAt;
  static TripPlace create(Trip trip,TripDay day,Place place,int order){var p=new TripPlace();p.trip=trip;p.day=day;p.place=place;p.visitOrder=order;return p;}
  void changeOrder(int order){visitOrder=order;}
  public void setReason(String reason){this.reason=reason;}
  public void relocate(TripDay target,int order){this.day.getPlaces().remove(this);this.day=target;this.visitOrder=order;target.getPlaces().add(this);}
  public void updateContent(String memo,List<FileMetadata> files){this.memo=memo;this.images.clear();for(int i=0;i<files.size();i++)this.images.add(TripPlaceImage.create(this,files.get(i),i));}
}
