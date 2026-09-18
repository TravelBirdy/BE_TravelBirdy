package com.travelbird.trip.entity;

import jakarta.persistence.*;
import java.util.*;
import lombok.*;

@Entity @Table(name="trip_days", uniqueConstraints={@UniqueConstraint(name="uk_trip_day_number", columnNames={"trip_id","day_number"}), @UniqueConstraint(name="uk_trip_day_trip_pair", columnNames={"trip_id","trip_day_id"})})
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TripDay {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="trip_day_id") private Long id;
  @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="trip_id", nullable=false) private Trip trip;
  @Column(name="day_number", nullable=false) private int dayNumber;
  @OneToMany(mappedBy="day", cascade=CascadeType.ALL, orphanRemoval=true) @OrderBy("visitOrder") private List<TripPlace> places=new ArrayList<>();
  TripDay(Trip trip,int dayNumber){this.trip=trip;this.dayNumber=dayNumber;}
  TripPlace addPlace(Long placeId,int order){var p=TripPlace.create(trip,this,placeId,order);places.add(p);return p;}
  void remove(TripPlace place){places.remove(place);}
  void normalize(){for(int i=0;i<places.size();i++) places.get(i).changeOrder(i+1);}
}
