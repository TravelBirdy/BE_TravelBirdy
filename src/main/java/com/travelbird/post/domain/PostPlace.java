package com.travelbird.post.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 게시글에 노출되는 경로 장소. Owner = Part 3. {@code tripPlaceId}는 Part 2 소유
 * ({@code trip_places})를 가리키는 scalar FK다 — 장소 상세(이름/카테고리/주소/좌표)는
 * 이 엔티티가 아니라 {@code place.api.PlaceReader}로, day/순서/메모는 Part2의
 * {@code TripPostReader}로 조회한다(직접 조회 금지).
 */
@Entity
@Table(name = "post_places")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostPlace {

    @EmbeddedId
    private PostPlaceId id;

    private PostPlace(PostPlaceId id) {
        this.id = id;
    }

    public static PostPlace of(Long postId, Long tripPlaceId) {
        return new PostPlace(new PostPlaceId(postId, tripPlaceId));
    }
}
