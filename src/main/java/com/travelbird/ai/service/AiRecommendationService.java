package com.travelbird.ai.service;

import com.travelbird.ai.dto.internal.*;
import com.travelbird.ai.dto.internal.PlaceSyncRequest.PlaceSyncItem;
import com.travelbird.ai.dto.request.*;
import com.travelbird.ai.dto.response.*;
import com.travelbird.ai.entity.*;
import com.travelbird.ai.repository.*;
import com.travelbird.global.error.*;
import com.travelbird.place.api.PlaceContract;
import com.travelbird.place.api.PlaceReader;
import com.travelbird.place.api.SavedPlaceReader;
import com.travelbird.region.api.RegionReader;
import com.travelbird.post.api.PostRouteLockReader;
import com.travelbird.common.enums.TravelTheme;
import com.travelbird.trip.repository.TripRepository;
import com.travelbird.user.api.UserReader;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiRecommendationService {
  private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
  private final AiRecommendationJobRepository jobs;
  private final UserReader users;
  private final SavedPlaceReader savedPlaces;
  private final TripRepository trips;
  private final RegionReader regions;
  private final PlaceReader places;
  private final PostRouteLockReader routeLocks;
  private final AiRecommendationDataReader data;
  private final RecommendationSnapshotCodec snapshots;
  private final RecommendationPayloadFingerprint fingerprints;
  private final ApplicationEventPublisher events;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();
  private AiTripPreviewRepository previews;

  public AiRecommendationService(AiRecommendationJobRepository jobs, UserReader users,
      SavedPlaceReader savedPlaces, TripRepository trips, RegionReader regions,
      PlaceReader places, PostRouteLockReader routeLocks,
      AiRecommendationDataReader data, RecommendationSnapshotCodec snapshots,
      RecommendationPayloadFingerprint fingerprints, ApplicationEventPublisher events, Clock clock) {
    this.jobs=jobs; this.users=users; this.savedPlaces=savedPlaces; this.trips=trips;
    this.regions=regions; this.places=places; this.routeLocks=routeLocks; this.data=data; this.snapshots=snapshots;
    this.fingerprints=fingerprints; this.events=events; this.clock=clock;
  }

  @Transactional
  public AiJobAcceptedResponse request(Long userId, AiRecommendationRequest input) {
    lockUserAndCheckQuota(userId);
    validateCommon(input.regionCode(), input.startDate(), input.endDate(), input.themes());
    List<Long> ids = input.savedPlaceIds() == null ? List.of() : List.copyOf(input.savedPlaceIds());
    rejectDuplicates(ids);
    List<PlaceContract> placeContracts = places.getPlaces(ids, userId);
    AiRequestType type = AiRequestType.valueOf(input.requestType().name());
    if (type == AiRequestType.GENERAL) {
      AiRecommendationPolicy.general(ids);
    } else {
      Set<Long> owned = savedPlaces.areAllSavedByUser(userId, ids) ? new HashSet<>(ids) : Set.of();
      Set<Long> regional = placeContracts.stream()
          .filter(p -> p.sigunguCode().equals(input.regionCode()))
          .map(PlaceContract::placeId).collect(Collectors.toSet());
      AiRecommendationPolicy.saved(ids, owned, regional);
    }
    return queue(userId, null, type, input.regionCode(), input.startDate(), input.endDate(),
        input.companionType(), input.themes(), input.pace(), ids, List.of(), List.of(), true, placeContracts);
  }

  @Transactional
  public AiJobAcceptedResponse requestTrip(Long userId, Long tripId, AiRouteRecommendationRequest input) {
    lockUserAndCheckQuota(userId);
    var trip = trips.findByIdForUpdate(tripId)
        .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND));
    if (!trip.ownedBy(userId)) throw new BusinessException(ErrorCode.TRIP_ACCESS_DENIED);
    trip.ensureMutable();
    var routeLock = routeLocks.findActivePublishedPostByTripId(tripId);
    if (routeLock != null && routeLock.routeLocked()) throw new BusinessException(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST);
    var schedule = trip.getDays().stream()
        .map(day -> new ExistingScheduleDay(day.getDayNumber(), day.getPlaces().stream()
            .map(place -> place.getPlaceId()).toList())).toList();
    List<Long> existing = schedule.stream().flatMap(day -> day.placeIds().stream()).toList();
    List<Long> wishlist = data.wishlistPlaceIds(tripId);
    AiRecommendationPolicy.trip(existing, wishlist, trip.getDays().size());
    Set<Long> all = new LinkedHashSet<>(existing); all.addAll(wishlist);
    return queue(userId, tripId, AiRequestType.TRIP_WISHLIST,
        trip.getRegionCode(), trip.getStartDate(), trip.getEndDate(),
        trip.getCompanionType(), new ArrayList<>(trip.getThemes()), trip.getPace(),
        List.of(), wishlist, schedule, input.allowAdditional(), places.getPlaces(new ArrayList<>(all), userId));
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  public void setPreviewRepository(AiTripPreviewRepository previews) { this.previews = previews; }

  @Transactional(readOnly = true)
  public AiJobStatusResponse status(Long userId, Long jobId) {
    var job = jobs.findById(jobId).orElseThrow(() -> new BusinessException(ErrorCode.AI_JOB_NOT_FOUND));
    if (!job.ownedBy(userId)) throw new BusinessException(ErrorCode.AI_JOB_ACCESS_DENIED);
    AiJobStatusResponse.AiJobError error = job.getErrorCode() == null ? null
        : new AiJobStatusResponse.AiJobError(job.getErrorCode(), job.getErrorMessage(),
            Boolean.TRUE.equals(job.getErrorRetryable()));
    AiTripPreview preview = previews == null ? null : previews.findByJobId(jobId).orElse(null);
    boolean available = preview != null && job.getStatus() == BackendAiJobStatus.SUCCEEDED;
    String retention = job.getStatus() == BackendAiJobStatus.EXPIRED ? "EXPIRED" : preview == null ? null : preview.getRetentionStatus().name();
    return new AiJobStatusResponse(jobId, job.getStatus(), preview == null ? null : preview.getId(), available, retention, available,
        job.getRequestedAt(), job.getStartedAt(), job.getCompletedAt(), preview == null ? null : preview.getExpiresAt(),
        job.getExpiredAt(), preview == null ? null : preview.getSavedAt(), error);
  }

  private void lockUserAndCheckQuota(Long userId) {
    users.validateActiveUser(userId);
    LocalDate koreaToday = LocalDate.now(clock.withZone(KOREA));
    LocalDateTime utcStart = koreaToday.atStartOfDay(KOREA).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    if (jobs.countSince(userId, utcStart) >= 3) throw new BusinessException(ErrorCode.AI_DAILY_REQUEST_LIMIT_EXCEEDED);
    return;
  }

  private void validateCommon(String region, LocalDate start, LocalDate end, List<TravelTheme> themes) {
    if (!regions.existsBySigunguCode(region)) throw new BusinessException(ErrorCode.REGION_REQUIRED);
    if (end.isBefore(start)) throw new BusinessException(ErrorCode.INVALID_TRIP_PERIOD);
    if (new HashSet<>(themes).size() != themes.size()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
  }

  private AiJobAcceptedResponse queue(Long uid, Long tripId, AiRequestType type, String region,
      LocalDate start, LocalDate end, com.travelbird.common.enums.CompanionType companion,
      List<TravelTheme> themes, com.travelbird.common.enums.Pace pace, List<Long> saved,
      List<Long> wishlist, List<ExistingScheduleDay> schedule, boolean additional, List<PlaceContract> places) {
    long id = positiveId();
    List<TravelTheme> normalizedThemes = themes.stream().sorted().toList();
    List<Long> normalizedSaved = saved.stream().sorted().toList();
    List<Long> normalizedWishlist = wishlist.stream().sorted().toList();
    List<ExistingScheduleDay> normalizedSchedule = schedule.stream()
        .sorted(Comparator.comparingInt(ExistingScheduleDay::dayNumber)).toList();
    var request = new RecommendationJobRequest(id, type, region, start, end, companion,
        normalizedThemes, pace, normalizedSaved, normalizedWishlist, normalizedSchedule, additional);
    LocalDateTime now = LocalDateTime.now(clock);
    jobs.save(AiRecommendationJob.queued(id, uid, tripId, type, region, start, end, companion, pace,
        snapshots.write(normalizedThemes), snapshots.write(normalizedSaved),
        snapshots.write(normalizedWishlist), snapshots.write(normalizedSchedule), additional, now));
    var sync = new PlaceSyncRequest(places.stream().map(this::syncItem).toList());
    events.publishEvent(new AiDispatchRequested(request, sync, fingerprints.sha256(request)));
    return new AiJobAcceptedResponse(id, "QUEUED", now, "/api/ai/trip-recommendations/" + id);
  }

  private PlaceSyncItem syncItem(PlaceContract place) {
    return new PlaceSyncItem(place.placeId(), place.sigunguCode(), place.name(),
        place.address(), place.latitude(), place.longitude(), place.category());
  }
  private void rejectDuplicates(List<Long> ids) {
    if (new HashSet<>(ids).size() != ids.size()) throw new BusinessException(ErrorCode.INVALID_REQUEST);
  }
  private long positiveId() {
    long id;
    do { id = random.nextLong() & Long.MAX_VALUE; } while (id == 0 || jobs.existsById(id));
    return id;
  }
}
