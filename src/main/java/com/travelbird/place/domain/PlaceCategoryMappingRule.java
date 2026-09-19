package com.travelbird.place.domain;

import com.travelbird.common.enums.PlaceCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 외부 검색 결과의 원문 카테고리를 {@link PlaceCategory}로 매핑하는 규칙. Owner = Part 3.
 * {@code priority} 오름차순으로 첫 매치를 사용한다 (backend-functional-spec-v10.md §3.6.1).
 * 초기 23개 행은 {@code V2__seed_place_category_mapping_rules.sql}로 채운다.
 */
@Entity
@Table(name = "place_category_mapping_rules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceCategoryMappingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "include_keyword", nullable = false)
    private String includeKeyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_category", nullable = false, length = 30)
    private PlaceCategory targetCategory;

    @Column(name = "excluded", nullable = false)
    private boolean excluded;
}
