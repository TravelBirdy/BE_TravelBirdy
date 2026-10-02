package com.travelbird.place.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.travelbird.common.enums.PlaceCategory;
import com.travelbird.common.util.LikePatterns;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceStatus;
import com.travelbird.place.domain.QPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 통합검색(§3.16.1) 장소 섹션 — canonical {@code places}(TourAPI 적재분)를 이름·주소로 검색한다.
 * 이름이 일치하는 장소를 주소만 일치하는 장소보다 먼저 보여준다. {@code sigunguCodesOrNull}이
 * {@code null}이면 전국(지역 필터 없음)이다.
 */
@Repository
@RequiredArgsConstructor
public class PlaceSearchRepository {

    private static final QPlace place = QPlace.place;

    private final JPAQueryFactory queryFactory;

    public List<Place> search(String query, List<String> sigunguCodesOrNull, int limit) {
        String pattern = LikePatterns.contains(query);
        BooleanExpression nameMatch = place.name.like(pattern, '\\');
        BooleanExpression addressMatch = place.address.like(pattern, '\\');

        return queryFactory.selectFrom(place)
                .where(place.status.eq(PlaceStatus.ACTIVE),
                        nameMatch.or(addressMatch),
                        sigunguFilter(sigunguCodesOrNull))
                .orderBy(new CaseBuilder().when(nameMatch).then(0).otherwise(1).asc(),
                        place.name.asc(), place.placeId.asc())
                .limit(limit)
                .fetch();
    }

    /**
     * 장소 검색 API(§3.6.1) — 카테고리 필터와 페이징을 지원한다. 좌표가 오면 같은 일치 단계(이름 일치
     * / 주소 일치) 안에서 가까운 순으로 정렬한다(정렬용이라 위·경도 차이의 제곱합으로 근사하고, 경도는
     * 위도에 따른 길이 보정을 한다).
     */
    public List<Place> search(String query, PlaceCategory categoryOrNull,
                               BigDecimal latitudeOrNull, BigDecimal longitudeOrNull, long offset, int limit) {
        String pattern = LikePatterns.contains(query);
        BooleanExpression nameMatch = place.name.like(pattern, '\\');

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        orders.add(new CaseBuilder().when(nameMatch).then(0).otherwise(1).asc());
        if (latitudeOrNull != null && longitudeOrNull != null) {
            orders.add(distanceScore(latitudeOrNull, longitudeOrNull).asc());
        }
        orders.add(place.name.asc());
        orders.add(place.placeId.asc());

        return queryFactory.selectFrom(place)
                .where(matches(query, categoryOrNull))
                .orderBy(orders.toArray(new OrderSpecifier<?>[0]))
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    public long count(String query, PlaceCategory categoryOrNull) {
        Long total = queryFactory.select(place.count()).from(place)
                .where(matches(query, categoryOrNull))
                .fetchOne();
        return total == null ? 0 : total;
    }

    private BooleanExpression[] matches(String query, PlaceCategory categoryOrNull) {
        String pattern = LikePatterns.contains(query);
        return new BooleanExpression[]{
                place.status.eq(PlaceStatus.ACTIVE),
                place.name.like(pattern, '\\').or(place.address.like(pattern, '\\')),
                categoryOrNull == null ? null : place.category.eq(categoryOrNull)};
    }

    private NumberExpression<BigDecimal> distanceScore(BigDecimal latitude, BigDecimal longitude) {
        BigDecimal lngScale = BigDecimal.valueOf(Math.cos(Math.toRadians(latitude.doubleValue())));
        NumberExpression<BigDecimal> dLat = place.latitude.subtract(latitude);
        NumberExpression<BigDecimal> dLng = place.longitude.subtract(longitude).multiply(lngScale);
        return dLat.multiply(dLat).add(dLng.multiply(dLng));
    }

    private BooleanExpression sigunguFilter(List<String> sigunguCodesOrNull) {
        return sigunguCodesOrNull == null ? null : place.sigunguCode.in(sigunguCodesOrNull);
    }
}
