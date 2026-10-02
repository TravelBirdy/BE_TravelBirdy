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
     *
     * <p>{@code clearAutomatically}는 쓰지 않는다 — 탈퇴 Orchestrator({@code WithdrawalService})는 같은
     * 트랜잭션에서 {@code User}를 먼저 조회해 두고 마지막에 {@code user.withdraw()}로 상태를 바꾸는데,
     * 중간에 영속성 컨텍스트를 비우면 그 엔티티가 준영속이 되어 {@code WITHDRAWN} 변경이 저장되지 않는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update PostShare s set s.userId = null where s.userId = :userId")
    int clearUser(@Param("userId") Long userId);
}
