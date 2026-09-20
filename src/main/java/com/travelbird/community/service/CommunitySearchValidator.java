package com.travelbird.community.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.global.error.BusinessException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.region.api.RegionReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 커뮤니티 검색(§3.9.3) 요청 파라미터 검증. {@code sigunguCodes} 유효성만 여기서
 * 확인한다 — 실제 검색 결과 좁히기는 {@code TripPostReader}(Part2, 미merge) 없이는
 * 불가능해서 아직 반영하지 않는다({@code CommunityPostSearchService}의 TODO 참고).
 */
@Component
@RequiredArgsConstructor
public class CommunitySearchValidator {

    private static final int QUERY_MAX_LENGTH = 50;

    private final RegionReader regionReader;

    public void validateQueryLength(String query) {
        if (query != null && query.length() > QUERY_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.SEARCH_QUERY_TOO_LONG);
        }
    }

    /**
     * {@code null}이면 전국(필터 없음)이라는 뜻으로 그대로 {@code null}을 반환한다.
     * 그 외에는 유효한 시군구 코드만 걸러서 반환한다(잘못된 코드는 조용히 제외, 빈
     * 배열 입력은 빈 배열 반환).
     */
    public List<String> resolveValidSigunguCodes(List<String> sigunguCodesOrNull) {
        if (sigunguCodesOrNull == null) {
            return null;
        }
        if (sigunguCodesOrNull.isEmpty()) {
            return List.of();
        }
        return regionReader.getRegions(sigunguCodesOrNull).stream()
                .map(RegionSummary::sigunguCode)
                .toList();
    }
}
