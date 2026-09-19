package com.travelbird.personality.domain;

import com.travelbird.user.domain.User;

import com.travelbird.common.enums.BirdType;
import com.travelbird.common.enums.PersonalityTrait;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "personality_submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class PersonalitySubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "submission_id")
    private Long submissionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_version", nullable = false)
    private PersonalityTest personalityTest;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PersonalitySubmissionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "selected_trait")
    private PersonalityTrait selectedTrait;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tied_traits")
    private List<PersonalityTrait> tiedTraits;

    @Enumerated(EnumType.STRING)
    @Column(name = "bird_type")
    private BirdType birdType;

    @Column(name = "gourmet_score", nullable = false)
    private Integer gourmetScore;

    @Column(name = "rest_score", nullable = false)
    private Integer restScore;

    @Column(name = "photo_score", nullable = false)
    private Integer photoScore;

    @Column(name = "activity_score", nullable = false)
    private Integer activityScore;

    @Column(name = "culture_score", nullable = false)
    private Integer cultureScore;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static PersonalitySubmission create(User user, PersonalityTest personalityTest) {
        PersonalitySubmission submission = new PersonalitySubmission();
        submission.user = user;
        submission.personalityTest = personalityTest;
        submission.status = PersonalitySubmissionStatus.PENDING_TIE_BREAKER;
        submission.gourmetScore = 0;
        submission.restScore = 0;
        submission.photoScore = 0;
        submission.activityScore = 0;
        submission.cultureScore = 0;
        return submission;
    }

    public void recordSingleWinner(Map<PersonalityTrait, Integer> scores, PersonalityTrait winner, BirdType birdType) {
        applyScores(scores);
        this.selectedTrait = winner;
        this.tiedTraits = null;
        this.birdType = birdType;
        this.status = PersonalitySubmissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void recordTie(Map<PersonalityTrait, Integer> scores, List<PersonalityTrait> tiedTraits) {
        applyScores(scores);
        this.selectedTrait = null;
        this.tiedTraits = tiedTraits;
        this.birdType = null;
        this.status = PersonalitySubmissionStatus.PENDING_TIE_BREAKER;
        this.completedAt = null;
    }

    public void resolveTieBreaker(PersonalityTrait selected, BirdType birdType) {
        this.selectedTrait = selected;
        this.birdType = birdType;
        this.status = PersonalitySubmissionStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    private void applyScores(Map<PersonalityTrait, Integer> scores) {
        this.gourmetScore = scores.getOrDefault(PersonalityTrait.GOURMET, 0);
        this.restScore = scores.getOrDefault(PersonalityTrait.REST, 0);
        this.photoScore = scores.getOrDefault(PersonalityTrait.PHOTO, 0);
        this.activityScore = scores.getOrDefault(PersonalityTrait.ACTIVITY, 0);
        this.cultureScore = scores.getOrDefault(PersonalityTrait.CULTURE, 0);
    }
}
