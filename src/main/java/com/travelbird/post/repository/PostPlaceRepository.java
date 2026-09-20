package com.travelbird.post.repository;

import com.travelbird.post.domain.PostPlace;
import com.travelbird.post.domain.PostPlaceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostPlaceRepository extends JpaRepository<PostPlace, PostPlaceId> {

    List<PostPlace> findByIdPostId(Long postId);

    void deleteByIdPostId(Long postId);
}
