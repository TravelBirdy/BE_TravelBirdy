package com.travelbird.social.repository;

import com.travelbird.social.domain.Follow;
import com.travelbird.social.domain.FollowId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    @Modifying
    @Query("delete from Follow f where f.id.followerUserId = :userId or f.id.followingUserId = :userId")
    void deleteAllInvolvingUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Follow f where f.id.followerUserId = :followerId and f.id.followingUserId = :followingId")
    void deleteByFollowerAndFollowing(@Param("followerId") Long followerId, @Param("followingId") Long followingId);

    long countByFollower_UserId(Long userId);

    long countByFollowing_UserId(Long userId);

    @Query("select f.id.followingUserId from Follow f where f.id.followerUserId = :userId")
    List<Long> findFollowingUserIds(@Param("userId") Long userId);

    @Query("select f from Follow f where f.id.followingUserId = :userId "
            + "and (:cursor is null or f.followedAt < :cursor) order by f.followedAt desc")
    List<Follow> findFollowers(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);

    @Query("select f from Follow f where f.id.followerUserId = :userId "
            + "and (:cursor is null or f.followedAt < :cursor) order by f.followedAt desc")
    List<Follow> findFollowings(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);
}
