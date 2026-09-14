package com.travelbird.personality.repository;

import com.travelbird.personality.domain.PersonalitySubmission;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalitySubmissionRepository extends JpaRepository<PersonalitySubmission, Long> {

    Optional<PersonalitySubmission> findByUser_UserId(Long userId);
}
