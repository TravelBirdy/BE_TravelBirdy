package com.travelbird.region.repository;

import com.travelbird.region.domain.SigunguMaster;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SigunguMasterRepository extends JpaRepository<SigunguMaster, String> {

    List<SigunguMaster> findAllBySigunguCodeIn(List<String> sigunguCodes);

    /** 통합검색(§3.16.1) 지역 섹션 — 시군구명 부분일치(파생 쿼리의 Containing은 LIKE 특수문자를 이스케이프한다). */
    List<SigunguMaster> findBySigunguNameContainingOrderBySigunguNameAscSigunguCodeAsc(String name, Pageable pageable);

    List<SigunguMaster> findBySigunguNameContainingAndSigunguCodeInOrderBySigunguNameAscSigunguCodeAsc(
            String name, List<String> sigunguCodes, Pageable pageable);
}
