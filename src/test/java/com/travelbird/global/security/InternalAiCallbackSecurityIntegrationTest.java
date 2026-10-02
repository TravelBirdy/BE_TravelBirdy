package com.travelbird.global.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.travelbird.global.config.InternalAiProperties;
import com.travelbird.user.domain.UserRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class InternalAiCallbackSecurityIntegrationTest {
    private static final String CALLBACK = "/internal/ai-callbacks/trip-recommendations";
    private static final String BODY = "{\"jobId\":9223372036854775807,\"status\":\"COMPLETED\"}";
    @Autowired MockMvc mvc;
    @Autowired InternalAiProperties properties;
    @Autowired JwtUtil jwt;

    @Test void correctInternalKeyReachesCallbackWithoutUserJwt() throws Exception {
        mvc.perform(post(CALLBACK).header("X-Internal-AI-Key", properties.internalKey())
                .contentType("application/json").content(BODY))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("AI_JOB_NOT_FOUND"));
    }
    @Test void missingOrWrongInternalKeyIsRejectedByInternalAuthentication() throws Exception {
        mvc.perform(post(CALLBACK).contentType("application/json").content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_INTERNAL_AI_KEY"));
        mvc.perform(post(CALLBACK).header("X-Internal-AI-Key", "incorrect-key")
                .contentType("application/json").content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_INTERNAL_AI_KEY"));
    }
    @Test void userJwtCannotReplaceInternalKey() throws Exception {
        mvc.perform(post(CALLBACK).header("Authorization", "Bearer " + jwt.createAccessToken(1L, UserRole.ROLE_USER))
                .contentType("application/json").content(BODY))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_INTERNAL_AI_KEY"));
    }
    @Test void internalKeyDoesNotAuthenticatePublicUserEndpoints() throws Exception {
        mvc.perform(post("/api/ai/trip-recommendations").header("X-Internal-AI-Key", properties.internalKey())
                .contentType("application/json").content("{}"))
            .andExpect(status().isUnauthorized());
    }
}
