package com.travelbird.ai.repository;
import com.travelbird.ai.entity.*;import jakarta.persistence.LockModeType;import java.util.*;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface AiTripPreviewRepository extends JpaRepository<AiTripPreview,Long>{@EntityGraph(attributePaths={"days","days.places","hashtags"}) @Query("select distinct p from AiTripPreview p where p.id=:id") Optional<AiTripPreview> findDetail(@Param("id")Long id);@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select p from AiTripPreview p where p.id=:id") Optional<AiTripPreview> findByIdForGate(@Param("id")Long id);Optional<AiTripPreview> findByJobId(Long jobId);List<AiTripPreview> findAllByUserId(Long userId);}


