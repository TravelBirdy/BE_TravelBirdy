package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_social_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccount {

    @EmbeddedId
    private UserSocialAccountId id;

    @Column(name = "user_id", nullable = false)
    private Long userId;
}
