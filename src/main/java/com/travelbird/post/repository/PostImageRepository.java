package com.travelbird.post.repository;

import com.travelbird.post.domain.PostImage;
import com.travelbird.post.domain.PostImageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostImageRepository extends JpaRepository<PostImage, PostImageId> {

    List<PostImage> findByIdPostIdOrderByDisplayOrderAsc(Long postId);

    void deleteByIdPostId(Long postId);
}
