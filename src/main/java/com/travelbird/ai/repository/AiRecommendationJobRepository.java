package com.travelbird.ai.repository;

import com.travelbird.ai.entity.*;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AiRecommendationJobRepository extends JpaRepository<AiRecommendationJob, Long> {
  @Query("select count(j) from AiRecommendationJob j where j.user.id=:userId and j.requestedAt>=:from")
  long countSince(@Param("userId") Long userId, @Param("from") LocalDateTime from);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select j from AiRecommendationJob j where j.id=:id")
  Optional<AiRecommendationJob> findByIdForUpdate(@Param("id") Long id);

  List<AiRecommendationJob> findByStatusAndStartedAtLessThanEqual(
      BackendAiJobStatus status, LocalDateTime threshold);
}
