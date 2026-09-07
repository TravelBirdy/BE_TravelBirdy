package com.travelbird.user.repository;

import com.travelbird.user.domain.UserSocialAccount;
import com.travelbird.user.domain.UserSocialAccountId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UserSocialAccountId> {
}
