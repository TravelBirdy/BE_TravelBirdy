package com.travelbird.ai.service;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.travelbird.global.error.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class AiRecommendationPolicyTest {
 @Test void generalRejectsSavedPlaces(){assertCode(ErrorCode.CONFLICTING_PLACE_POLICY,()->AiRecommendationPolicy.general(List.of(1L)));}
 @Test void savedPlacesRequiresOwnedRegionalSelection(){assertCode(ErrorCode.INSUFFICIENT_SAVED_PLACES,()->AiRecommendationPolicy.saved(List.of(),Set.of(),Set.of()));assertCode(ErrorCode.SAVED_PLACE_ACCESS_DENIED,()->AiRecommendationPolicy.saved(List.of(1L),Set.of(),Set.of(1L)));assertCode(ErrorCode.INCOMPATIBLE_PLACE_REGIONS,()->AiRecommendationPolicy.saved(List.of(1L),Set.of(1L),Set.of()));}
 @Test void tripWishlistRequiresWishlistAndCapacity(){assertCode(ErrorCode.EMPTY_WISHLIST,()->AiRecommendationPolicy.trip(List.of(),List.of(),1));assertCode(ErrorCode.AI_ROUTE_REQUIRED_PLACE_LIMIT_EXCEEDED,()->AiRecommendationPolicy.trip(java.util.stream.LongStream.rangeClosed(1,15).boxed().toList(),List.of(16L),1));}
 private void assertCode(ErrorCode c,org.assertj.core.api.ThrowableAssert.ThrowingCallable f){assertThatThrownBy(f).isInstanceOf(BusinessException.class).extracting(e->((BusinessException)e).errorCode()).isEqualTo(c);}
}
