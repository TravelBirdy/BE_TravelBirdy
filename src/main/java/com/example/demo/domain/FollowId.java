package com.example.demo.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FollowId implements Serializable {

    private Long followerUserId;
    private Long followingUserId;

    public FollowId(Long followerUserId, Long followingUserId) {
        this.followerUserId = followerUserId;
        this.followingUserId = followingUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId that)) return false;
        return Objects.equals(followerUserId, that.followerUserId)
                && Objects.equals(followingUserId, that.followingUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerUserId, followingUserId);
    }
}
