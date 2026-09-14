package com.travelbird.common.service;

import com.travelbird.common.dto.RegionSummary;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link RegionReader}의 임시 구현. sigunguName을 알 방법이 없어 sigunguCode를
 * 그대로 표시용 이름 자리에 채운다(빈 문자열/null 대신 최소한의 비어있지 않은
 * 값을 보장하기 위함). Part 3의 sigungu_master 실 구현이 merge되면 이 Bean을
 * 지우고 교체한다 (공통협의 섹션4 Mock/Fake 병행 개발 규칙).
 */
@Component
public class NoOpRegionReader implements RegionReader {

    @Override
    public RegionSummary getRegion(String sigunguCode) {
        return new RegionSummary(sigunguCode, sigunguCode);
    }

    @Override
    public List<RegionSummary> getRegions(List<String> sigunguCodes) {
        return sigunguCodes.stream().distinct().map(this::getRegion).toList();
    }
}
