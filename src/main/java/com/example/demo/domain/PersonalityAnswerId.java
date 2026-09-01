package com.example.demo.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalityAnswerId implements Serializable {

    private Long submissionId;
    private Long questionId;

    public PersonalityAnswerId(Long submissionId, Long questionId) {
        this.submissionId = submissionId;
        this.questionId = questionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonalityAnswerId that)) return false;
        return Objects.equals(submissionId, that.submissionId)
                && Objects.equals(questionId, that.questionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(submissionId, questionId);
    }
}
