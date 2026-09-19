package com.travelbird.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.global.config.InternalAiProperties;
import com.travelbird.global.error.ErrorCode;
import com.travelbird.global.error.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalAiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTERNAL_AI_KEY_HEADER = "X-Internal-AI-Key";
    private static final RequestMatcher INTERNAL_PATH_MATCHER =
        new AntPathRequestMatcher("/internal/**");

    private final InternalAiProperties properties;
    private final ObjectMapper objectMapper;

    public InternalAiKeyAuthenticationFilter(
        InternalAiProperties properties,
        ObjectMapper objectMapper
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (!INTERNAL_PATH_MATCHER.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(INTERNAL_AI_KEY_HEADER);
        if (!matchesInternalKey(providedKey)) {
            writeUnauthorized(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean matchesInternalKey(String providedKey) {
        if (providedKey == null) {
            return false;
        }
        byte[] expected = properties.internalKey().getBytes(StandardCharsets.UTF_8);
        byte[] provided = providedKey.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.INVALID_INTERNAL_AI_KEY.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(
            response.getWriter(),
            ErrorResponse.of(ErrorCode.INVALID_INTERNAL_AI_KEY)
        );
    }
}
