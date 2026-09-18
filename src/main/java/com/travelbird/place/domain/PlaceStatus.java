package com.travelbird.place.domain;

/**
 * 장소 활성 상태. 폐업·비활성 장소는 삭제하지 않고 CLOSED로 유지한다. (기능명세 3.6.2, travelbird.dbml)
 */
public enum PlaceStatus {
	ACTIVE,
	CLOSED
}
