package com.travelbird.global.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class SecurityConfig {

    private static final List<String> ALLOWED_METHODS = List.of(
        HttpMethod.GET.name(),
        HttpMethod.POST.name(),
        HttpMethod.PUT.name(),
        HttpMethod.DELETE.name(),
        HttpMethod.PATCH.name(),
        HttpMethod.OPTIONS.name()
    );

    /*
     * Browser public API에서 필요한 header만 허용한다.
     * X-Internal-AI-Key는 브라우저가 전송하면 안 되므로 CORS allowed header에 포함하지 않는다.
     */
    private static final List<String> ALLOWED_HEADERS = List.of(
        HttpHeaders.AUTHORIZATION,
        HttpHeaders.CONTENT_TYPE,
        HttpHeaders.ACCEPT,
        HttpHeaders.ORIGIN
    );

    /*
     * 현재 공개 API 계약에는 custom response header 의존성이 없다.
     * 201 리소스 생성에서 Location을 사용할 수 있도록 Location만 명시적으로 노출한다.
     */
    private static final List<String> EXPOSED_HEADERS = List.of(
        HttpHeaders.LOCATION
    );

    private static final long CORS_MAX_AGE_SECONDS = Duration.ofHours(1).toSeconds();

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        CorsConfigurationSource corsConfigurationSource
    ) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // Preflight must not require JWT.
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Explicit no-auth endpoints from OpenAPI.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/kakao/login",
                    "/api/auth/token/refresh"
                ).permitAll()

                // Optional-auth read APIs.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/trips/*",
                    "/api/posts/*",
                    "/api/community/posts",
                    "/api/community/posts/search",
                    "/api/home",
                    "/api/events",
                    "/api/events/*",
                    "/api/trips/*/days/*/route"
                ).permitAll()

                // Optional-auth metric APIs.
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/posts/*/views",
                    "/api/posts/*/shares"
                ).permitAll()

                /*
                 * Backend inbound AI Callback은 Bearer JWT 대상이 아니다.
                 * 이 path를 활성화할 때는 X-Internal-AI-Key 전용 인증 Filter를
                 * SecurityFilterChain에 함께 연결해야 한다.
                 * Filter가 없는 상태에서 이 matcher를 permitAll로 추가하지 않는다.
                 */

                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        CorsProperties corsProperties
    ) {
        if (corsProperties.allowedOrigins().isEmpty()) {
            throw new IllegalStateException(
                "app.cors.allowed-origins must contain at least one explicit origin"
            );
        }

        if (corsProperties.allowedOrigins().stream().anyMatch("*"::equals)) {
            throw new IllegalStateException(
                "Wildcard CORS origin '*' is not allowed when allowCredentials=true"
            );
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
