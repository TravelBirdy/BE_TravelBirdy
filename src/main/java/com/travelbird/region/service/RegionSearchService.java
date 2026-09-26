package com.travelbird.region.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.region.domain.SigunguMaster;
import com.travelbird.region.repository.SigunguMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 통합검색(§3.16.1) 지역 섹션. 공개 계약 {@code RegionReader}에는 검색 메서드가 없고 이 검색은
 * Part3 내부 용도라 계약을 늘리지 않고 별도 서비스로 둔다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionSearchService {

    private final SigunguMasterRepository sigunguMasterRepository;

    /** {@code sigunguCodesOrNull}이 {@code null}이면 전국. */
    public List<RegionSummary> search(String query, List<String> sigunguCodesOrNull, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<SigunguMaster> found = sigunguCodesOrNull == null
                ? sigunguMasterRepository.findBySigunguNameContainingOrderBySigunguNameAscSigunguCodeAsc(query, pageable)
                : sigunguMasterRepository.findBySigunguNameContainingAndSigunguCodeInOrderBySigunguNameAscSigunguCodeAsc(
                        query, sigunguCodesOrNull, pageable);
        return found.stream()
                .map(region -> new RegionSummary(region.getSigunguCode(), region.getSigunguName()))
                .toList();
    }
}
