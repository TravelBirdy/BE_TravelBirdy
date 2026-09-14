package com.travelbird.personality.repository;

import com.travelbird.personality.domain.PersonalityQuestion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalityQuestionRepository extends JpaRepository<PersonalityQuestion, Long> {

    List<PersonalityQuestion> findByPersonalityTest_TestVersionOrderByQuestionOrderAsc(String testVersion);
}
