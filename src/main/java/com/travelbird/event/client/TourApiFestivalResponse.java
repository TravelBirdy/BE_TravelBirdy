package com.travelbird.event.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 한국관광공사 TourAPI 4.0 searchFestival2 응답 구조.
 * 프로젝트 문서에 없는 외부 공개 API 규격(https://api.visitkorea.or.kr)을 바탕으로 작성했으며,
 * 실제 서비스키로 호출해 응답 필드를 검증하기 전까지는 참고용으로만 신뢰한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiFestivalResponse(
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Items items, Integer totalCount) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String contentid,
            String title,
            String addr1,
            String firstimage,
            String eventstartdate,
            String eventenddate,
            // 법정동 시도/시군구 코드. 둘을 이어붙이면 우리 프로젝트의 5자리
            // sigunguCode와 동일하다는 걸 실제 응답으로 검증했다(예: 11+680=11680 강남구).
            String lDongRegnCd,
            String lDongSignguCd
    ) {
    }
}
