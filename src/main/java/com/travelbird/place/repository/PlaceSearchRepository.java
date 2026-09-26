package com.travelbird.place.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.travelbird.common.util.LikePatterns;
import com.travelbird.place.domain.Place;
import com.travelbird.place.domain.PlaceStatus;
import com.travelbird.place.domain.QPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

    private BooleanExpression sigunguFilter(List<String> sigunguCodesOrNull) {
        return sigunguCodesOrNull == null ? null : place.sigunguCode.in(sigunguCodesOrNull);
    }
}
