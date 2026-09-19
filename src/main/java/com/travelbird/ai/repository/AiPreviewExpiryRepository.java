package com.travelbird.ai.repository;
import com.travelbird.ai.entity.AiTripPreview;import java.time.LocalDateTime;import java.util.List;import org.springframework.data.jpa.repository.*;import org.springframework.data.repository.query.Param;
public interface AiPreviewExpiryRepository extends JpaRepository<AiTripPreview,Long>{@Query("select p.id from AiTripPreview p where p.retentionStatus=com.travelbird.ai.entity.AiPreviewRetentionStatus.TEMPORARY and p.expiresAt<=:now")List<Long> findExpiredIds(@Param("now")LocalDateTime now);}
