package com.travelbird.trip.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelbird.global.security.JwtUtil;
import com.travelbird.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TripResponseSerializationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtUtil jwt;
    @Autowired ObjectMapper json;
    @Test void detailSerializesThemesAndHashtagsAfterServiceTransactionEnds() throws Exception {
        jdbc.update("insert into users (user_id,role) values (81,'ROLE_USER')");
        String auth="Bearer "+jwt.createAccessToken(81L,UserRole.ROLE_USER);
        var result=mvc.perform(post("/api/trips").header("Authorization",auth).contentType("application/json")
            .content("{\"regionCode\":\"11110\",\"startDate\":\"2026-10-10\",\"endDate\":\"2026-10-10\",\"companionType\":\"SOLO\",\"themes\":[\"FOOD\"],\"pace\":\"NORMAL\"}"))
            .andExpect(status().isCreated()).andReturn();
        long id=json.readTree(result.getResponse().getContentAsString()).get("tripId").asLong();
        mvc.perform(get("/api/trips/{id}",id).header("Authorization",auth))
            .andExpect(status().isOk()).andExpect(jsonPath("$.themes[0]").value("FOOD"))
            .andExpect(jsonPath("$.hashtags").isArray());
    }
}
