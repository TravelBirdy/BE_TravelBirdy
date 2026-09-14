package com.travelbird.common.service;

import com.travelbird.common.dto.RegionSummary;
import java.util.List;

/**
 * Part 3 → Part 1(Event/Home) · Part 2(Trip/AI) 공개 계약 (공통협의 4.3절).
 * {@code sigungu_master}의 Owner는 Part 3. Part 3 실 구현이 이 저장소에
 * merge되기 전까지는 {@link NoOpRegionReader}가 자리를 대신한다.
 */
public interface RegionReader {

    RegionSummary getRegion(String sigunguCode);

    List<RegionSummary> getRegions(List<String> sigunguCodes);
}
