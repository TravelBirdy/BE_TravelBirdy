package com.travelbird.personality.repository;

import com.travelbird.personality.domain.PersonalityAnswer;
import com.travelbird.personality.domain.PersonalityAnswerId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PersonalityAnswerRepository extends JpaRepository<PersonalityAnswer, PersonalityAnswerId> {

    @Modifying
    @Query("delete from PersonalityAnswer a where a.submission.submissionId = :submissionId")
    void deleteAllBySubmissionId(@Param("submissionId") Long submissionId);
}
