package com.travelbird.place.repository;

import com.travelbird.place.domain.PlaceExternalId;
import com.travelbird.place.domain.PlaceExternalIdProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceExternalIdRepository extends JpaRepository<PlaceExternalId, Long> {

    Optional<PlaceExternalId> findByProviderAndExternalPlaceId(PlaceExternalIdProvider provider,
                                                                 String externalPlaceId);
}
