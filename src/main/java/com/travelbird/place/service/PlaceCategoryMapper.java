package com.travelbird.place.service;

import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.place.domain.PlaceCategoryMappingRule;
import com.travelbird.place.repository.PlaceCategoryMappingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 원문 카테고리/키워드를 {@link PlaceCategory}로 분류한다. backend-functional-spec-v10.md
 * §3.6.1 — priority 오름차순 첫 매치 사용, 매치 없으면 {@code OTHER}, {@code excluded} 규칙에
 * 매치되면 결과에서 제외한다({@link Optional#empty()}).
 */
@Service
@RequiredArgsConstructor
public class PlaceCategoryMapper {

    private final PlaceCategoryMappingRuleRepository placeCategoryMappingRuleRepository;

    public Optional<PlaceCategory> categorize(String rawCategoryOrKeyword) {
        List<PlaceCategoryMappingRule> rules = placeCategoryMappingRuleRepository.findAllByOrderByPriorityAsc();

        if (rawCategoryOrKeyword != null) {
            for (PlaceCategoryMappingRule rule : rules) {
                if (rawCategoryOrKeyword.contains(rule.getIncludeKeyword())) {
                    return rule.isExcluded() ? Optional.empty() : Optional.of(rule.getTargetCategory());
                }
            }
        }
        return Optional.of(PlaceCategory.OTHER);
    }
}
