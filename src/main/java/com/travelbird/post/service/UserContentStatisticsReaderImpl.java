package com.travelbird.post.service;

import com.travelbird.photomap.service.PhotoMapService;
import com.travelbird.post.api.UserContentStatistics;
import com.travelbird.post.api.UserContentStatisticsReader;
import com.travelbird.post.domain.PostStatus;
import com.travelbird.post.repository.PostRepository;
import com.travelbird.trip.api.TripPostReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 마이페이지 집계용 {@link UserContentStatisticsReader} 실구현(공통협의 4.3절, 기능명세서
 * §3.14.1). {@code postCount}는 삭제되지 않은 Post 중 DRAFT·BLOCKED를 제외한
 * {@code PUBLISHED}만 센다(PRIVATE 포함). {@code visitedRegionCount}는 포토맵과 같은
 * 집계({@link PhotoMapService#countVisitedRegions})를 그대로 써서 두 화면 숫자가 어긋나지
 * 않게 한다. Part1의 {@code NoOpUserContentStatisticsReader}는
 * {@code @ConditionalOnMissingBean}이라 이 Bean이 등록되면 자동으로 밀려난다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserContentStatisticsReaderImpl implements UserContentStatisticsReader {

    private final PostRepository postRepository;
    private final TripPostReader tripPostReader;
    private final PhotoMapService photoMapService;

    @Override
    public UserContentStatistics getStatistics(Long userId) {
        List<Long> tripIds = tripPostReader.getTripIdsByUser(userId);
        long postCount = tripIds.isEmpty()
                ? 0
                : postRepository.countByTripIdInAndStatusAndDeletedAtIsNull(tripIds, PostStatus.PUBLISHED);
        return new UserContentStatistics(postCount, photoMapService.countVisitedRegions(userId));
    }
}
