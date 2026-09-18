package com.travelbird.trip.entity;
import com.travelbird.file.entity.FileMetadata; import jakarta.persistence.*; import java.io.Serializable; import lombok.*;
@Entity @Table(name="trip_place_images") @IdClass(TripPlaceImage.Id.class) @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TripPlaceImage {
 @jakarta.persistence.Id @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_place_id") private TripPlace tripPlace;
 @jakarta.persistence.Id @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="file_id") private FileMetadata file;
 @Column(name="display_order",nullable=false) private int displayOrder;
 static TripPlaceImage create(TripPlace p,FileMetadata f,int order){var i=new TripPlaceImage();i.tripPlace=p;i.file=f;i.displayOrder=order;return i;}
 @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode public static class Id implements Serializable {private Long tripPlace;private Long file;}
}
