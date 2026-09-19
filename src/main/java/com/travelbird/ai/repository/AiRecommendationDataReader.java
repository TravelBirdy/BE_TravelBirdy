package com.travelbird.ai.repository;
import com.travelbird.trip.entity.TripWishlistPlace;import jakarta.persistence.EntityManager;import java.util.*;import org.springframework.stereotype.Repository;
@Repository public class AiRecommendationDataReader{private final EntityManager em;public AiRecommendationDataReader(EntityManager em){this.em=em;}public List<Long> wishlistPlaceIds(Long tripId){return em.createQuery("select w.placeId from TripWishlistPlace w where w.trip.id=:id order by w.placeId",Long.class).setParameter("id",tripId).getResultList();}}
