package com.travelbird.home.repository;

import com.travelbird.home.domain.HomeRecommendedPlace;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomeRecommendedPlaceRepository extends JpaRepository<HomeRecommendedPlace, Long> {

    List<HomeRecommendedPlace> findAllByOrderByDisplayOrderAsc();
}
