package com.example.demo.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlockId implements Serializable {

    private Long blockerUserId;
    private Long blockedUserId;

    public UserBlockId(Long blockerUserId, Long blockedUserId) {
        this.blockerUserId = blockerUserId;
        this.blockedUserId = blockedUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBlockId that)) return false;
        return Objects.equals(blockerUserId, that.blockerUserId)
                && Objects.equals(blockedUserId, that.blockedUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockerUserId, blockedUserId);
    }
}
