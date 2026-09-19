package com.travelbird.user.repository;

import com.travelbird.user.domain.UserSocialAccount;
import com.travelbird.user.domain.UserSocialAccountId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UserSocialAccountId> {

    @Modifying
    @Query("delete from UserSocialAccount u where u.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
