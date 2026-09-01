package com.example.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "personality_questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalityQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_version", nullable = false)
    private PersonalityTest personalityTest;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;
}
