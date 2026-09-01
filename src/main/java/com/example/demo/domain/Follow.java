package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "follows")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Follow {

    @EmbeddedId
    private FollowId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followerUserId")
    @JoinColumn(name = "follower_user_id")
    private User follower;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("followingUserId")
    @JoinColumn(name = "following_user_id")
    private User following;

    @CreatedDate
    @Column(name = "followed_at", nullable = false, updatable = false)
    private LocalDateTime followedAt;
}
