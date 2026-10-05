package com.splitmate.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class HealthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldReturnOkStatusWithoutLogin() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.status").value("OK"));
    }
}
