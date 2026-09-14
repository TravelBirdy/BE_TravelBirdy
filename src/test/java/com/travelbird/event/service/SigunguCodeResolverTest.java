package com.travelbird.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SigunguCodeResolverTest {

    private final SigunguCodeResolver resolver = new SigunguCodeResolver();

    @Test
    void resolveFromAddress_seoulJongno_returnsCorrectCode() {
        assertThat(resolver.resolveFromAddress("서울특별시 종로구 세종로 1"))
                .contains("11110");
    }

    @Test
    void resolveFromAddress_changwonUichang_matchesLongestPrefix() {
        assertThat(resolver.resolveFromAddress("경상남도 창원시 의창구 중앙대로 1"))
                .contains("48121");
    }

    @Test
    void resolveFromAddress_sejong_returnsSingleTierCode() {
        assertThat(resolver.resolveFromAddress("세종특별자치시 한누리대로 1"))
                .contains("36110");
    }

    @Test
    void resolveFromAddress_unknown_returnsEmpty() {
        assertThat(resolver.resolveFromAddress("존재하지않는시도 어딘가구")).isEmpty();
    }

    @Test
    void resolveFromAddress_null_returnsEmpty() {
        assertThat(resolver.resolveFromAddress(null)).isEmpty();
    }
}
