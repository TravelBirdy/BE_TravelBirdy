package com.travelbird.savedroute.service;

import com.travelbird.post.api.PostSaveStatusReader;
import com.travelbird.savedroute.repository.SavedRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostSaveStatusReaderImpl implements PostSaveStatusReader {

    private final SavedRouteRepository savedRouteRepository;

    @Override
    public Set<Long> findSavedPostIds(Long viewerIdOrNull, List<Long> postIds) {
        if (viewerIdOrNull == null || postIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(savedRouteRepository.findSavedPostIds(viewerIdOrNull, postIds));
    }
}
