package com.travelbird.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.global.config.InternalAiProperties;
import com.travelbird.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class InternalAiKeyAuthenticationFilterTest {

    private static final String INTERNAL_AI_KEY_HEADER = "X-Internal-AI-Key";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final InternalAiKeyAuthenticationFilter filter =
        new InternalAiKeyAuthenticationFilter(
            new InternalAiProperties("expected-internal-key"),
            objectMapper
        );

    @Test
    void nonInternalPathPassesWithoutInternalKey() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(
            request("GET", "/api/home"),
            response,
            new MockFilterChain()
);

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void validInternalKeyPassesInternalCallback() throws Exception {
        MockHttpServletRequest request = request(
            "POST",
            "/internal/ai-callbacks/trip-recommendations"
);
        request.addHeader(INTERNAL_AI_KEY_HEADER, "expected-internal-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void missingInternalKeyStopsWithInvalidInternalAiKey() throws Exception {
        MockHttpServletRequest request = request(
            "POST",
            "/internal/ai-callbacks/trip-recommendations"
);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
            .isEqualTo(ErrorCode.INVALID_INTERNAL_AI_KEY.name());
    }

    @Test
    void invalidInternalKeyStopsWithInvalidInternalAiKey() throws Exception {
        MockHttpServletRequest request = request(
            "POST",
            "/internal/v1/recommendations"
);
        request.addHeader(INTERNAL_AI_KEY_HEADER, "wrong-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(response.getContentAsString()).get("code").asText())
            .isEqualTo(ErrorCode.INVALID_INTERNAL_AI_KEY.name());
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
