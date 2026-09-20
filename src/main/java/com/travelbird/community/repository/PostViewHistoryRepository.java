package com.travelbird.community.repository;

import com.travelbird.community.domain.PostViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface PostViewHistoryRepository extends JpaRepository<PostViewHistory, Long> {

    /** 조회수 증가 API(§3.9.4)가 도입되면 24시간 중복 판정에 쓸 편의 메서드. 현재는 미사용. */
    long countByPostIdAndViewedAtBetween(Long postId, LocalDateTime from, LocalDateTime to);
}
