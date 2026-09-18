package com.travelbird.trip.mapper;

import com.travelbird.region.dto.response.RegionSummary;
import com.travelbird.trip.dto.response.*;
import com.travelbird.trip.entity.*;
import java.time.LocalDate;
import java.util.List;
import com.travelbird.file.storage.ObjectStorage;
import com.travelbird.trip.service.TripImagePolicy;
import org.springframework.stereotype.Component;

@Component
public class TripMapper {
 private final ObjectStorage storage; public TripMapper(ObjectStorage storage){this.storage=storage;}
 public CreateTripResponse created(Trip t,LocalDate today){return new CreateTripResponse(t.getId(),t.getTitle(),t.getSourceType(),t.status(today),t.getCancelledAt(),t.getVisibility(),region(t),t.getStartDate(),t.getEndDate(),t.getCompanionType(),t.getThemes(),t.getPace(),t.getHashtags(),days(t,false),true,null);}
 public TripResponse detail(Trip t,Long viewerId,Long publishedPostId,LocalDate today){boolean owner=t.ownedBy(viewerId);boolean cancelled=t.getCancelledAt()!=null;boolean locked=publishedPostId!=null;boolean mask=!owner&&t.getVisibility()==Visibility.MEMO_PRIVATE;return new TripResponse(t.getId(),new AuthorSummary(t.getUser().getId(),t.getUser().getNickname(),t.getUser().getBirdType()),t.getTitle(),t.getSummary(),t.getSourceType(),t.status(today),t.getCancelledAt(),t.getVisibility(),region(t),t.getStartDate(),t.getEndDate(),t.getPace(),t.getCompanionType(),t.getThemes(),t.getHashtags(),owner&&!cancelled&&!locked,owner?(cancelled?"TRIP_CANCELLED":locked?"PUBLISHED_POST_EXISTS":null):null,owner?publishedPostId:null,owner&&!cancelled,days(t,mask));}
 public TripListItem list(Trip t,LocalDate today){return new TripListItem(t.getId(),t.getTitle(),t.getStartDate(),t.getEndDate(),t.status(today),region(t));}
 public TravelCalendarItem calendar(Trip t,LocalDate today){return new TravelCalendarItem(t.getId(),t.getTitle(),t.getStartDate(),t.getEndDate(),t.status(today));}
 public RouteResponse route(Trip t,TripDay d){var ps=d.getPlaces().stream().map(p->new RoutePlace(p.getId(),p.getVisitOrder(),p.getPlace().getId(),p.getPlace().getLatitude(),p.getPlace().getLongitude())).toList();return new RouteResponse(t.getId(),d.getDayNumber(),ps,ps.stream().map(p->new Coordinates(p.latitude(),p.longitude())).toList());}
 private RegionSummary region(Trip t){return new RegionSummary(t.getRegion().getSigunguCode(),t.getRegion().getSigunguName());}
 private List<TripDayResponse> days(Trip t,boolean mask){return t.getDays().stream().map(d->new TripDayResponse(d.getDayNumber(),d.getPlaces().stream().map(p->new TripPlaceResponse(p.getId(),p.getPlace().getId(),p.getVisitOrder(),mask?null:p.getMemo(),mask,p.getImages().stream().map(i->TripImagePolicy.summarize(i.getFile(),storage)).toList(),p.getReason(),new Coordinates(p.getPlace().getLatitude(),p.getPlace().getLongitude()))).toList())).toList();}
}
