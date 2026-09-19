package com.travelbird.place.repository;

import com.travelbird.place.domain.PlaceCategoryMappingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceCategoryMappingRuleRepository extends JpaRepository<PlaceCategoryMappingRule, Long> {

    List<PlaceCategoryMappingRule> findAllByOrderByPriorityAsc();
}
