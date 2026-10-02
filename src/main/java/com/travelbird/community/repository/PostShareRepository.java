package com.travelbird.community.repository;

import com.travelbird.community.domain.PostShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostShareRepository extends JpaRepository<PostShare, Long> {

    /**
     * 회원 탈퇴 시 공유 로그는 통계로 남기되 사용자 식별만 끊는다 — {@code post_shares.user_id}는
     * nullable(비로그인 공유와 동일 취급, FK도 SET NULL).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update PostShare s set s.userId = null where s.userId = :userId")
    int clearUser(@Param("userId") Long userId);
}
