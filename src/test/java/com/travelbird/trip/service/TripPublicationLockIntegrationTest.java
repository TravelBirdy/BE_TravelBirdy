package com.travelbird.trip.service;

import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostVisibility;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import com.travelbird.trip.dto.request.AddTripPlaceRequest;
import java.sql.SQLException;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class TripPublicationLockIntegrationTest {
  @org.springframework.test.context.bean.override.mockito.MockitoSpyBean TripPostReader reader;
  @Autowired com.travelbird.post.service.PostCreateService postCreateService;
  @Autowired TripService tripService;
  @Autowired TripAiRouteService aiRouteService;
  @Autowired PostRepository posts;
  @Autowired JdbcTemplate jdbc;
  @Autowired DataSource dataSource;
  @Autowired PlatformTransactionManager transactionManager;
  TransactionTemplate tx;
  Long tripId;

  @BeforeEach void seed() {
    tx = new TransactionTemplate(transactionManager);
    jdbc.update("insert ignore into sigungu_master (sigungu_code,sigungu_name) values ('11110','Jongno')");
    jdbc.update("insert ignore into users (user_id,role) values (1,'ROLE_USER')");
    tripId = tx.execute(status -> {
      jdbc.update("insert into trips (user_id,source_type,title,sigungu_code,start_date,end_date,companion_type,pace) values (1,'MANUAL','Lock test','11110','2026-09-25','2026-09-25','SOLO','RELAXED')");
      return jdbc.queryForObject("select last_insert_id()", Long.class);
    });
  }

  private TripPostReader.TripPostSnapshot lock(Long userId, Long id) {
    return reader.lockOwnedTripForPost(userId, id);
  }

  @Test void requiresCallerTransaction() {
    assertThatThrownBy(() -> lock(1L, tripId)).isInstanceOf(IllegalTransactionStateException.class);
  }

  @Test void preservesOwnershipAndNotFound() {
    assertThatThrownBy(() -> tx.execute(s -> lock(2L, tripId)))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_ACCESS_DENIED));
    assertThatThrownBy(() -> tx.execute(s -> lock(1L, Long.MAX_VALUE)))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_NOT_FOUND));
  }

  @Test void realRowLockSurvivesReaderReturnAndPostFlushUntilCommit() {
    tx.executeWithoutResult(s -> {
      assertThat(lock(1L, tripId).tripId()).isEqualTo(tripId);
      assertThat(competingRowLock()).isEqualTo(1205);
      posts.saveAndFlush(Post.create(tripId,"Title","Content",null,PostVisibility.PUBLIC,true));
      assertThat(competingRowLock()).isEqualTo(1205);
    });
    assertThat(competingRowLock()).isZero();
    assertThatThrownBy(() -> tripService.addPlace(1L,tripId,1,new AddTripPlaceRequest(999L,null)))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
    assertThatThrownBy(() -> tripService.cancel(1L,tripId))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_CANCEL_REQUIRES_POST_DELETION));
  }

  @Test void rollbackReleasesLockAndDoesNotPublish() {
    tx.executeWithoutResult(s -> {
      lock(1L,tripId);
      posts.saveAndFlush(Post.create(tripId,"Title","Content",null,PostVisibility.PUBLIC,true));
      assertThat(competingRowLock()).isEqualTo(1205);
      s.setRollbackOnly();
    });
    assertThat(competingRowLock()).isZero();
    assertThat(posts.findByTripIdAndDeletedAtIsNull(tripId)).isEmpty();
    tripService.cancel(1L,tripId);
  }

  @Test void ordinaryDraftReaderDoesNotAcquirePublicationLock() {
    tx.executeWithoutResult(s -> {
      assertThat(reader.getOwnedTripForPost(1L,tripId).ownerUserId()).isEqualTo(1L);
      assertThat(competingRowLock()).isZero();
      posts.saveAndFlush(Post.create(tripId,null,null,null,PostVisibility.PRIVATE,false));
    });
    tripService.cancel(1L,tripId);
  }

  @Test void cancelledSnapshotRetainsExistingPolicy() {
    tripService.cancel(1L,tripId);
    assertThat(tx.execute(s -> lock(1L,tripId)).cancelledAt()).isNotNull();
    assertThat(reader.getOwnedTripForPost(1L,tripId).cancelledAt()).isNotNull();
    assertThatThrownBy(() -> tripService.addPlace(1L,tripId,1,new AddTripPlaceRequest(999L,null)))
        .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo(ErrorCode.TRIP_CANCELLED_READ_ONLY));
  }

  @Test void actualDraftCreateDoesNotCallPublicationLock() {
    org.mockito.Mockito.clearInvocations(reader);
    var response = postCreateService.create(1L, new com.travelbird.post.controller.dto.CreatePostRequest(
        tripId,null,null,null,java.util.List.of(),java.util.List.of(),java.util.List.of(),PostVisibility.PRIVATE,false));
    assertThat(response.publishedAt()).isNull();
    org.mockito.Mockito.verify(reader).getOwnedTripForPost(1L,tripId);
    org.mockito.Mockito.verify(reader,org.mockito.Mockito.never()).lockOwnedTripForPost(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
  }

  @Test void publicationSnapshotSeesRouteCommittedAfterEarlierRead() throws Exception {
    try (var executor = Executors.newSingleThreadExecutor()) {
      tx.executeWithoutResult(s -> {
        jdbc.queryForObject("select count(*) from trips", Long.class); // establish RR snapshot
        try {
          executor.submit(() -> tx.executeWithoutResult(other ->
              jdbc.update("insert into trip_days (trip_id,day_number) values (?,1)",tripId))).get(10,TimeUnit.SECONDS);
        } catch (Exception e) { throw new AssertionError(e); }
        assertThat(lock(1L,tripId).days()).hasSize(1);
      });
    }
  }

  @Test void routeMutationWaitsForPublicationThenRejects() throws Exception {
    var attempted = new CountDownLatch(1);
    try (var executor = Executors.newSingleThreadExecutor()) {
      var future = new java.util.concurrent.atomic.AtomicReference<Future<?>>();
      tx.executeWithoutResult(s -> {
        lock(1L,tripId);
        future.set(executor.submit(() -> {
          attempted.countDown();
          tripService.addPlace(1L,tripId,1,new AddTripPlaceRequest(999L,null));
        }));
        try {
          assertThat(attempted.await(5,TimeUnit.SECONDS)).isTrue();
          assertThatThrownBy(() -> future.get().get(300,TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
        posts.saveAndFlush(Post.create(tripId,"Title","Content",null,PostVisibility.PUBLIC,true));
      });
      assertThatThrownBy(() -> future.get().get(10,TimeUnit.SECONDS))
          .hasCauseInstanceOf(BusinessException.class)
          .satisfies(e -> assertThat(((BusinessException)e.getCause()).errorCode())
              .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
    }
  }

  @Test void mutationSeesPublicationCommittedAfterEarlierRead() {
    try (var executor = Executors.newSingleThreadExecutor()) {
      tx.executeWithoutResult(s -> {
        jdbc.queryForObject("select count(*) from posts", Long.class);
        try {
          executor.submit(() -> tx.executeWithoutResult(other -> {
            lock(1L,tripId);
            posts.saveAndFlush(Post.create(tripId,"Title","Content",null,PostVisibility.PUBLIC,true));
          })).get(10,TimeUnit.SECONDS);
        } catch (Exception e) { throw new AssertionError(e); }
        assertThatThrownBy(() -> tripService.cancel(1L,tripId))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                .isEqualTo(ErrorCode.TRIP_CANCEL_REQUIRES_POST_DELETION));
        assertThatThrownBy(() -> tripService.addPlace(1L,tripId,1,new AddTripPlaceRequest(999L,null)))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
        assertThatThrownBy(() -> tripService.removePlace(1L,tripId,1,999L))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
        assertThatThrownBy(() -> tripService.reorder(1L,tripId,1,null))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
        var preview = org.mockito.Mockito.mock(com.travelbird.ai.entity.AiTripPreview.class);
        var job = org.mockito.Mockito.mock(com.travelbird.ai.entity.AiRecommendationJob.class);
        org.mockito.Mockito.when(preview.getJob()).thenReturn(job);
        org.mockito.Mockito.when(job.getTargetTripId()).thenReturn(tripId);
        assertThatThrownBy(() -> aiRouteService.apply(preview,1L))
            .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                .isEqualTo(ErrorCode.TRIP_ROUTE_LOCKED_BY_PUBLISHED_POST));
        s.setRollbackOnly();
      });
    }
  }

  @Test void currentSnapshotPreservesEmptyDaysMemoAndImageOrder() {
    tx.executeWithoutResult(s -> {
      jdbc.update("insert into trip_themes (trip_id,theme) values (?,'NATURE')",tripId);
      jdbc.update("insert into trip_days (trip_id,day_number) values (?,1)",tripId);
      Long dayId = jdbc.queryForObject("select last_insert_id()",Long.class);
      jdbc.update("insert into trip_days (trip_id,day_number) values (?,2)",tripId);
      jdbc.update("insert into places (status,name,category,address,sigungu_code,latitude,longitude) values ('ACTIVE','Place','ATTRACTION','Address','11110',37.5,127.0)");
      Long placeId = jdbc.queryForObject("select last_insert_id()",Long.class);
      jdbc.update("insert into trip_places (trip_id,trip_day_id,place_id,visit_order,memo) values (?,?,?,1,'Memo')",tripId,dayId,placeId);
      Long tripPlaceId = jdbc.queryForObject("select last_insert_id()",Long.class);
      var images = new java.util.ArrayList<Long>();
      for (int i=0; i<2; i++) {
        jdbc.update("insert into files (user_id,file_name,object_key,content_type,size_bytes,width,height,purpose,status,expires_at) values (1,'a.jpg',?,'image/jpeg',1,1,1,'TRIP_PLACE','UPLOADED',now())",java.util.UUID.randomUUID().toString());
        images.add(jdbc.queryForObject("select last_insert_id()",Long.class));
      }
      jdbc.update("insert into trip_place_images (trip_place_id,file_id,display_order) values (?,?,0),(?,?,1)",tripPlaceId,images.get(1),tripPlaceId,images.get(0));
      var snapshot = lock(1L,tripId);
      assertThat(snapshot.themes()).containsExactly("NATURE");
      assertThat(snapshot.days()).extracting(TripPostReader.TripPostDaySnapshot::dayNumber).containsExactly(1,2);
      assertThat(snapshot.days().get(1).places()).isEmpty();
      assertThat(snapshot.days().get(0).places()).containsExactly(new TripPostReader.TripPostPlaceSnapshot(
          tripPlaceId,placeId,1,1,"Memo",java.util.List.of(images.get(1),images.get(0))));
    });
  }

  @Test void readOnlyTripDetailSupportsCurrentPostLookup() {
    assertThat(tripService.detail(1L,tripId).routeEditable()).isTrue();
    tx.executeWithoutResult(s -> posts.saveAndFlush(
        Post.create(tripId,"Title","Content",null,PostVisibility.PUBLIC,true)));
    assertThat(tripService.detail(1L,tripId).routeEditable()).isFalse();
  }

  private int competingRowLock() {
    try (var executor = Executors.newSingleThreadExecutor()) {
      return executor.submit(() -> {
        try (var connection = dataSource.getConnection()) {
          connection.setAutoCommit(false);
          try (var statement = connection.createStatement()) {
            statement.execute("set session innodb_lock_wait_timeout=1");
            try {
              statement.executeQuery("select trip_id from trips where trip_id=" + tripId + " for update").close();
              return 0;
            } catch (SQLException e) {
              if (e.getErrorCode() != 1205) throw e;
              return e.getErrorCode();
            } finally {
              connection.rollback();
              statement.execute("set session innodb_lock_wait_timeout=50");
              connection.setAutoCommit(true);
            }
          }
        }
      }).get(10,TimeUnit.SECONDS);
    } catch (Exception e) { throw new AssertionError(e); }
  }
}
