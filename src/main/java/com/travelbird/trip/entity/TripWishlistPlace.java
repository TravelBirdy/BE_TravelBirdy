package com.travelbird.trip.entity;


import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.*;

@Entity @Table(name="trip_wishlist_places") @IdClass(TripWishlistPlace.Id.class)
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED)
public class TripWishlistPlace {
  @jakarta.persistence.Id @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="trip_id") private Trip trip;
  @jakarta.persistence.Id @Column(name="place_id") private Long placeId;
  @Column(name="added_at",nullable=false) private LocalDateTime addedAt;
  public static TripWishlistPlace create(Trip trip,Long placeId,LocalDateTime now){var w=new TripWishlistPlace();w.trip=trip;w.placeId=placeId;w.addedAt=now;return w;}
  @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode public static class Id implements Serializable { private Long trip; private Long placeId; }
}
