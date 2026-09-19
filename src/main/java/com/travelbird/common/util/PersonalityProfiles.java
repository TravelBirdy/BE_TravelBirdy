package com.travelbird.common.util;

import com.travelbird.common.enums.BirdType;
import com.travelbird.common.enums.PersonalityTrait;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 성향(PersonalityTrait) - 파트너 새(BirdType) 고정 매핑 (기능명세서 3.3.2).
 * DB에 저장되지 않는 서버 상수라 온보딩/파트너 새/소셜(팔로우 목록) 등
 * 여러 곳에서 공유해서 쓴다.
 */
public final class PersonalityProfiles {

    public record TraitProfile(BirdType birdType, String birdName, String description) {
    }

    public static final Map<PersonalityTrait, TraitProfile> TRAIT_PROFILES = Map.of(
            PersonalityTrait.REST, new TraitProfile(BirdType.OMOKNUNI, "오목눈이", "힐링/휴양"),
            PersonalityTrait.ACTIVITY, new TraitProfile(BirdType.MULCHONGSAE, "물총새", "액티비티/모험"),
            PersonalityTrait.CULTURE, new TraitProfile(BirdType.HOBANSAE, "호반새", "문화/예술"),
            PersonalityTrait.GOURMET, new TraitProfile(BirdType.DDAKSAE, "딱새", "미식/맛집"),
            PersonalityTrait.PHOTO, new TraitProfile(BirdType.DONGBAKSAE, "동박새", "감성/기록")
    );

    private static final Map<BirdType, PersonalityTrait> TRAIT_BY_BIRD_TYPE = TRAIT_PROFILES.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getValue().birdType(), Map.Entry::getKey));

    private PersonalityProfiles() {
    }

    public static PersonalityTrait traitFor(BirdType birdType) {
        return birdType == null ? null : TRAIT_BY_BIRD_TYPE.get(birdType);
    }
}
