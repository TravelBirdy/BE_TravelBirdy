package com.travelbird.trip.mapper;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.file.api.FileLinkService;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.trip.dto.response.*;
import com.travelbird.trip.entity.*;
import com.travelbird.user.api.UserReader;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {
  private final UserReader users;
  private final RegionReader regions;
  private final PlaceReader places;
  private final FileLinkService files;

  public TripMapper(
      UserReader users, RegionReader regions, PlaceReader places, FileLinkService files) {
    this.users = users;
    this.regions = regions;
    this.places = places;
    this.files = files;
  }

  public CreateTripResponse created(Trip trip, LocalDate today) {
    return new CreateTripResponse(
        trip.getId(), trip.getTitle(), trip.getSourceType(), trip.status(today),
        trip.getCancelledAt(), trip.getVisibility(), region(trip), trip.getStartDate(),
        trip.getEndDate(), trip.getCompanionType(), trip.getThemes(), trip.getPace(),
        trip.getHashtags(), days(trip, false), true, null);
  }

  public TripResponse detail(Trip trip, Long viewerId, Long publishedPostId, LocalDate today) {
    boolean owner = trip.ownedBy(viewerId);
    boolean cancelled = trip.getCancelledAt() != null;
    boolean locked = publishedPostId != null;
    boolean mask = !owner && trip.getVisibility() == Visibility.MEMO_PRIVATE;
    var author = users.getUserSummary(trip.getUserId());
    return new TripResponse(
        trip.getId(), new AuthorSummary(author.userId(), author.nickname(), author.birdType()),
        trip.getTitle(), trip.getSummary(), trip.getSourceType(), trip.status(today),
        trip.getCancelledAt(), trip.getVisibility(), region(trip), trip.getStartDate(),
        trip.getEndDate(), trip.getPace(), trip.getCompanionType(), trip.getThemes(),
        trip.getHashtags(), owner && !cancelled && !locked,
        owner ? (cancelled ? "TRIP_CANCELLED" : locked ? "PUBLISHED_POST_EXISTS" : null) : null,
        owner ? publishedPostId : null, owner && !cancelled, days(trip, mask));
  }

  public TripListItem list(Trip trip, LocalDate today) {
    return new TripListItem(
        trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate(),
        trip.status(today), region(trip));
  }

  public TravelCalendarItem calendar(Trip trip, LocalDate today) {
    return new TravelCalendarItem(
        trip.getId(), trip.getTitle(), trip.getStartDate(), trip.getEndDate(), trip.status(today));
  }

  public RouteResponse route(Trip trip, TripDay day) {
    var routePlaces = day.getPlaces().stream().map(place -> {
      PlaceContract contract = places.getPlace(place.getPlaceId(), null);
      return new RoutePlace(
          place.getId(), place.getVisitOrder(), place.getPlaceId(),
          contract.latitude(), contract.longitude());
    }).toList();
    return new RouteResponse(
        trip.getId(), day.getDayNumber(), routePlaces,
        routePlaces.stream().map(p -> new Coordinates(p.latitude(), p.longitude())).toList());
  }

  private RegionSummary region(Trip trip) {
    return regions.getRegion(trip.getRegionCode());
  }

  private List<TripDayResponse> days(Trip trip, boolean mask) {
    return trip.getDays().stream().map(day -> new TripDayResponse(
        day.getDayNumber(), day.getPlaces().stream().map(place -> {
          PlaceContract contract = places.getPlace(place.getPlaceId(), null);
          var fileIds = place.getImages().stream().map(TripPlaceImage::getFileId).toList();
          var urls = files.getImageUrls(fileIds);
          var images = fileIds.stream()
              .filter(urls::containsKey)
              .map(fileId -> new ImageSummary(fileId, urls.get(fileId)))
              .toList();
          return new TripPlaceResponse(
              place.getId(), place.getPlaceId(), place.getVisitOrder(),
              mask ? null : place.getMemo(), mask, images, place.getReason(),
              new Coordinates(contract.latitude(), contract.longitude()));
        }).toList())).toList();
  }
}
