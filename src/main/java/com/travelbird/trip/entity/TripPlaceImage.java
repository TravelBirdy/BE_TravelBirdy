package com.travelbird.trip.entity;
import jakarta.persistence.*; import java.io.Serializable; import lombok.*;
@Entity @Table(name="trip_place_images") @IdClass(TripPlaceImage.Id.class) @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TripPlaceImage {
 @jakarta.persistence.Id @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_place_id") private TripPlace tripPlace;
 @jakarta.persistence.Id @Column(name="file_id",nullable=false) private Long fileId;
 @Column(name="display_order",nullable=false) private int displayOrder;
 static TripPlaceImage create(TripPlace p,Long fileId,int order){var i=new TripPlaceImage();i.tripPlace=p;i.fileId=fileId;i.displayOrder=order;return i;}
 @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode public static class Id implements Serializable {private Long tripPlace;private Long fileId;}
}
