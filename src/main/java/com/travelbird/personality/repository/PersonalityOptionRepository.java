package com.travelbird.personality.repository;

import com.travelbird.personality.domain.PersonalityOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalityOptionRepository extends JpaRepository<PersonalityOption, Long> {

    List<PersonalityOption> findByQuestion_QuestionIdIn(List<Long> questionIds);
}
