package com.travelbird.personality.repository;

import com.travelbird.personality.domain.PersonalityTest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalityTestRepository extends JpaRepository<PersonalityTest, String> {

    Optional<PersonalityTest> findByActiveTrue();
}
