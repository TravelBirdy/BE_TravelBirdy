package com.travelbird.social.repository;

import com.travelbird.social.domain.UserBlock;
import com.travelbird.social.domain.UserBlockId;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    @Modifying
    @Query("delete from UserBlock b where b.id.blockerUserId = :userId or b.id.blockedUserId = :userId")
    void deleteAllInvolvingUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from UserBlock b where b.id.blockerUserId = :blockerId and b.id.blockedUserId = :blockedId")
    void deleteByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    @Query("select case when count(b) > 0 then true else false end from UserBlock b "
            + "where (b.id.blockerUserId = :userId and b.id.blockedUserId = :targetUserId) "
            + "or (b.id.blockerUserId = :targetUserId and b.id.blockedUserId = :userId)")
    boolean existsEitherDirection(@Param("userId") Long userId, @Param("targetUserId") Long targetUserId);

    @Query("select case when b.id.blockerUserId = :userId then b.id.blockedUserId else b.id.blockerUserId end "
            + "from UserBlock b where b.id.blockerUserId = :userId or b.id.blockedUserId = :userId")
    List<Long> findBlockedUserIdsEitherDirection(@Param("userId") Long userId);

    @Query("select b from UserBlock b where b.id.blockerUserId = :userId "
            + "and (:cursor is null or b.blockedAt < :cursor) order by b.blockedAt desc")
    List<UserBlock> findBlockedByBlocker(@Param("userId") Long userId, @Param("cursor") LocalDateTime cursor, Pageable pageable);
}
