package com.travelbird.community.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.region.api.RegionReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommunitySearchValidatorTest {

    /** 실제 DB 없이 코드->존재여부만 흉내내는 최소 Fake — {@code getRegions}만 이 테스트에서 쓴다. */
    private static class FakeRegionReader implements RegionReader {
        private final Map<String, String> namesByCode;

        FakeRegionReader(Map<String, String> namesByCode) {
            this.namesByCode = namesByCode;
        }

        @Override
        public boolean existsBySigunguCode(String sigunguCode) {
            return namesByCode.containsKey(sigunguCode);
        }

        @Override
        public RegionSummary getRegion(String sigunguCode) {
            throw new UnsupportedOperationException("이 테스트에서는 사용하지 않음");
        }

        @Override
        public List<RegionSummary> getRegions(List<String> sigunguCodes) {
            return sigunguCodes.stream()
                    .filter(namesByCode::containsKey)
                    .map(code -> new RegionSummary(code, namesByCode.get(code)))
                    .toList();
        }
    }

    private final CommunitySearchValidator validator =
            new CommunitySearchValidator(new FakeRegionReader(Map.of("11110", "종로구", "26110", "중구")));

    @Test
    void 검색어가_50자면_통과한다() {
        String query = "가".repeat(50);
        validator.validateQueryLength(query);
    }

    @Test
    void 검색어가_51자면_예외를_던진다() {
        String query = "가".repeat(51);
        assertThatThrownBy(() -> validator.validateQueryLength(query))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.SEARCH_QUERY_TOO_LONG));
    }

    @Test
    void sigunguCodes가_null이면_null을_그대로_반환한다() {
        assertThat(validator.resolveValidSigunguCodes(null)).isNull();
    }

    @Test
    void sigunguCodes가_빈배열이면_빈배열을_반환한다() {
        assertThat(validator.resolveValidSigunguCodes(List.of())).isEmpty();
    }

    @Test
    void 유효하지_않은_코드는_걸러진다() {
        List<String> resolved = validator.resolveValidSigunguCodes(List.of("11110", "99999"));
        assertThat(resolved).containsExactly("11110");
    }
}
