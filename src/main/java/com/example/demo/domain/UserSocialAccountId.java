package com.example.demo.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSocialAccountId implements Serializable {

    private String provider;
    private String providerUserId;

    public UserSocialAccountId(String provider, String providerUserId) {
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSocialAccountId that)) return false;
        return Objects.equals(provider, that.provider)
                && Objects.equals(providerUserId, that.providerUserId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider, providerUserId);
    }
}
