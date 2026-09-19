package com.travelbird.post.repository;

import com.travelbird.post.domain.Post;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.domain.PostVisibility;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    boolean existsByTripIdAndDeletedAtIsNull(Long tripId);

    Optional<Post> findByTripIdAndDeletedAtIsNull(Long tripId);

    Optional<Post> findByPostIdAndDeletedAtIsNull(Long postId);

    /** 인기 점수(조회수×1+저장수×3+공유수×5) 내림차순 후보군. {@code HomePostReaderImpl}가 셔플 전 pool로 사용. */
    @Query("select p from Post p where p.status = :status and p.deletedAt is null and p.visibility in :visibilities "
            + "order by (p.viewCount + p.saveCount * 3 + p.shareCount * 5) desc")
    List<Post> findHomeCandidates(@Param("status") PostStatus status,
                                   @Param("visibilities") List<PostVisibility> visibilities,
                                   Pageable pageable);
}
