package com.travelbird.post.repository;

import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPlaceRepository extends JpaRepository<PostPlace, PostPlaceId> {

    List<PostPlace> findByIdPostId(Long postId);

    /** 포토맵 집계용 배치 조회. backend-functional-spec-v10.md §3.11. */
    List<PostPlace> findByIdPostIdIn(List<Long> postIds);

    void deleteByIdPostId(Long postId);
}
