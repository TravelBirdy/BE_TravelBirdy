package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personality_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalityAnswer {

    @EmbeddedId
    private PersonalityAnswerId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("submissionId")
    @JoinColumn(name = "submission_id")
    private PersonalitySubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("questionId")
    @JoinColumn(name = "question_id")
    private PersonalityQuestion question;

    @Column(name = "option_id", nullable = false)
    private Long optionId;
}
