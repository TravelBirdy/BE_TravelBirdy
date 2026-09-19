package com.travelbird.region.repository;

import com.travelbird.region.domain.SigunguMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SigunguMasterRepository extends JpaRepository<SigunguMaster, String> {

    List<SigunguMaster> findAllBySigunguCodeIn(List<String> sigunguCodes);
}
