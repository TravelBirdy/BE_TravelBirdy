package com.travelbird.region.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 시군구 기준정보. Owner = Part 3. (공통협의 4.3절 RegionReader, backend-functional-spec-v10.md
 * 시군구 코드 기준 §1.4)
 *
 * <p>자연키({@code sigungu_code})라서 {@code @GeneratedValue}를 쓰지 않는다 — 초기 269개
 * 시군구는 전부 Flyway seed로 채워지며, 애플리케이션 코드가 새 행을 만들 일이 없다.
 */
@Entity
@Table(name = "sigungu_master")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SigunguMaster {

    @Id
    @Column(name = "sigungu_code", length = 5)
    private String sigunguCode;

    @Column(name = "sigungu_name", length = 100, nullable = false)
    private String sigunguName;

    private SigunguMaster(String sigunguCode, String sigunguName) {
        this.sigunguCode = sigunguCode;
        this.sigunguName = sigunguName;
    }

    public static SigunguMaster of(String sigunguCode, String sigunguName) {
        return new SigunguMaster(sigunguCode, sigunguName);
    }
}
