package com.travelbird.post.repository;

import com.travelbird.post.domain.PostHashtag;
import com.travelbird.post.domain.PostHashtagId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtagId> {

    List<PostHashtag> findByIdPostId(Long postId);

    void deleteByIdPostId(Long postId);
}
