package com.travelbird.place.repository;

import com.travelbird.place.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    List<Place> findAllByPlaceIdIn(List<Long> placeIds);
}
