package com.travelbird.user.domain;

import com.travelbird.common.enums.BirdType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "nickname", length = 10)
    private String nickname;

    @Column(name = "introduction", length = 100)
    private String introduction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "bird_type")
    private BirdType birdType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static User createFromKakao(String email) {
        User user = new User();
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.role = UserRole.ROLE_USER;
        user.onboardingCompleted = false;
        return user;
    }

    public void updateProfile(String nickname, String introduction) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (introduction != null) {
            this.introduction = introduction;
        }
    }

    public void assignBirdType(BirdType birdType) {
        this.birdType = birdType;
        this.onboardingCompleted = true;
    }

    public void withdraw() {
        this.email = null;
        this.nickname = null;
        this.introduction = null;
        this.birdType = null;
        this.onboardingCompleted = false;
        this.status = UserStatus.WITHDRAWN;
    }
}
