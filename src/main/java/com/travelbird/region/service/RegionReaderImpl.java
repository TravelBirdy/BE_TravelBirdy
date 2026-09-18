package com.travelbird.region.service;

import com.travelbird.common.dto.RegionSummary;
import com.travelbird.global.error.ApiException;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.region.api.RegionReader;
import com.travelbird.region.domain.SigunguMaster;
import com.travelbird.region.repository.SigunguMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegionReaderImpl implements RegionReader {

    private final SigunguMasterRepository sigunguMasterRepository;

    @Override
    public boolean existsBySigunguCode(String sigunguCode) {
        return sigunguMasterRepository.existsById(sigunguCode);
    }

    @Override
    public RegionSummary getRegion(String sigunguCode) {
        SigunguMaster sigunguMaster = sigunguMasterRepository.findById(sigunguCode)
                .orElseThrow(() -> new ApiException(ErrorCode.REGION_NOT_FOUND));
        return toSummary(sigunguMaster);
    }

    @Override
    public List<RegionSummary> getRegions(List<String> sigunguCodes) {
        return sigunguMasterRepository.findAllBySigunguCodeIn(sigunguCodes).stream()
                .map(RegionReaderImpl::toSummary)
                .toList();
    }

    private static RegionSummary toSummary(SigunguMaster sigunguMaster) {
        return new RegionSummary(sigunguMaster.getSigunguCode(), sigunguMaster.getSigunguName());
    }
}
