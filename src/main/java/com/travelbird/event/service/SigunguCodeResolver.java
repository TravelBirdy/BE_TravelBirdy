package com.travelbird.event.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * TourAPI 주소 문자열(addr1, "시도 시군구 상세주소")을 프로젝트 내부 5자리 시군구 코드로
 * 변환하는 보조 수단. 행정표준코드관리시스템(법정동코드) 원본에서 추출한
 * {@code data/sigungu-codes.csv}를 기준으로 한다.
 *
 * <p>TourAPI searchFestival2 응답의 {@code lDongRegnCd}+{@code lDongSignguCd}가
 * 우리 5자리 코드와 정확히 일치함을 실제 호출로 확인했으므로, 그게 있으면 그쪽을
 * 우선 쓰고 이 클래스는 값이 비어있을 때의 fallback으로만 사용한다.
 */
@Component
public class SigunguCodeResolver {

    private record Entry(String sido, String sigungu, String code) {
    }

    private final List<Entry> entries;
    private final Set<String> validCodes;

    public SigunguCodeResolver() {
        this.entries = loadEntries();
        this.validCodes = entries.stream().map(Entry::code).collect(Collectors.toSet());
    }

    public boolean exists(String sigunguCode) {
        return sigunguCode != null && validCodes.contains(sigunguCode);
    }

    public Optional<String> resolveFromAddress(String address) {
        if (address == null) {
            return Optional.empty();
        }
        String normalized = address.trim();
        for (Entry entry : entries) {
            // 세종특별자치시처럼 시군구 하위 분할이 없는 단일 행정구역은 시도명 자체가
            // 시군구명과 동일하게 저장돼 있어, 중복 없이 시도명 한 번만으로 매칭한다.
            String prefix = entry.sido().equals(entry.sigungu())
                    ? entry.sido()
                    : entry.sido() + " " + entry.sigungu();
            if (normalized.startsWith(prefix)) {
                return Optional.of(entry.code());
            }
        }
        return Optional.empty();
    }

    private List<Entry> loadEntries() {
        try (InputStream in = getClass().getResourceAsStream("/data/sigungu-codes.csv")) {
            if (in == null) {
                throw new IllegalStateException("data/sigungu-codes.csv 리소스를 찾을 수 없습니다.");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                return reader.lines()
                        .skip(1)
                        .filter(line -> !line.isBlank())
                        .map(line -> line.split(",", 3))
                        .map(parts -> new Entry(parts[1], parts[2], parts[0]))
                        // 시군구명이 긴 것부터 검사해야 "창원시 의창구"가 "창원시"보다 먼저 매칭된다.
                        .sorted(Comparator.comparingInt((Entry e) -> e.sigungu().length()).reversed())
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
