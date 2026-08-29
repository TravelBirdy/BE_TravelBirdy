package com.travelbird.user.entity;

public enum UserStatus {
    ACTIVE,
    SUSPENDED,
    WITHDRAWN;

    public boolean isActive() {
        return this == ACTIVE;
    }
}