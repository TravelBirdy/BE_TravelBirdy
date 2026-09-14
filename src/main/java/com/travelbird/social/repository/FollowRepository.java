package com.travelbird.social.repository;

import com.travelbird.social.domain.Follow;
import com.travelbird.social.domain.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Modifying
    @Query("delete from Follow f where f.id.followerUserId = :userId or f.id.followingUserId = :userId")
    void deleteAllInvolvingUser(@Param("userId") Long userId);
}
