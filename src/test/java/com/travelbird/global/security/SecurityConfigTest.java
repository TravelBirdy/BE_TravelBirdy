package com.travelbird.global.security;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@SpringJUnitWebConfig(SecurityConfigTest.TestConfig.class)
class SecurityConfigTest {

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class,
            ProbeController.class})
    static class TestConfig {

        @Bean
        JwtUtil jwtUtil() {
            return new JwtUtil("security-config-test-secret-key-0123456789-0123456789-abcdef", 1800, 2592000);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules();
        }
    }

    @RestController
    static class ProbeController {

        @GetMapping({"/api/home", "/api/events", "/api/events/{id}", "/api/trips/{id}",
                "/api/trips/{id}/days/{day}/route", "/api/posts/{id}", "/api/community/posts",
                "/api/community/posts/search", "/api/users/me"})
        String get() {
            return "ok";
        }

        @PostMapping({"/api/posts/{id}/views", "/api/posts/{id}/shares", "/api/reports"})
        String post() {
            return "ok";
        }
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 비로그인_허용_GET_엔드포인트는_토큰_없이_접근된다() throws Exception {
        String[] paths = {"/api/home", "/api/events", "/api/events/1", "/api/trips/1", "/api/trips/1/days/2/route",
                "/api/posts/1", "/api/community/posts", "/api/community/posts/search"};
        for (String path : paths) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void 비로그인_허용_POST_엔드포인트는_토큰_없이_접근된다() throws Exception {
        mockMvc.perform(post("/api/posts/1/views")).andExpect(status().isOk());
        mockMvc.perform(post("/api/posts/1/shares")).andExpect(status().isOk());
    }

    @Test
    void 같은_경로라도_허용되지_않은_메서드는_401이다() throws Exception {
        mockMvc.perform(delete("/api/trips/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/trips/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/posts/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/community/posts")).andExpect(status().isUnauthorized());
    }

    @Test
    void 허용목록에_없는_엔드포인트는_토큰_없으면_401이다() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/trips")).andExpect(status().isUnauthorized());
    }
}
