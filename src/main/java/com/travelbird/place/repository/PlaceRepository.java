package com.travelbird.place.repository;

import com.travelbird.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByPlaceIdIn(List<Long> placeIds);

    List<Place> findAllBySigunguCode(String sigunguCode);

    /** resolve 후보 조회용 좌표 bounding box — 요청에 지역 코드가 없어 좌표 근방만 좁힌다. */
    List<Place> findByLatitudeBetweenAndLongitudeBetween(BigDecimal minLatitude, BigDecimal maxLatitude,
                                                           BigDecimal minLongitude, BigDecimal maxLongitude);
}
