package com.travelbird.post.service;

import com.travelbird.post.api.PostRouteLock;
import com.travelbird.post.api.PostRouteLockReader;

import com.travelbird.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostRouteLockReaderImpl implements PostRouteLockReader {

    private final PostRepository postRepository;

    @Override
    public PostRouteLock findActivePublishedPostByTripId(Long tripId) {
        return postRepository.findCurrentRouteLockByTripId(tripId)
                .map(post -> new PostRouteLock(post.getPostId(), post.getPublishedAt() != null))
                .orElse(new PostRouteLock(null, false));
    }
}
