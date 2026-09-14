package com.travelbird.social.repository;

import com.travelbird.social.domain.UserBlock;
import com.travelbird.social.domain.UserBlockId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    @Modifying
    @Query("delete from UserBlock b where b.id.blockerUserId = :userId or b.id.blockedUserId = :userId")
    void deleteAllInvolvingUser(@Param("userId") Long userId);
}
