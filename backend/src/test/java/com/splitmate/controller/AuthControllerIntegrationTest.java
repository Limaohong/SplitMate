package com.splitmate.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmate.AbstractIntegrationTest;
import com.splitmate.dto.LoginRequest;
import com.splitmate.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void shouldRegisterLoginAndGetCurrentUser() throws Exception {
        String email = createUniqueEmail();
        performRegister(new RegisterRequest(email, VALID_PASSWORD, "Alice"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(email));

        // email 大小寫不同仍應登入成功
        String loginResponseBody = performLogin(new LoginRequest(email.toUpperCase(), VALID_PASSWORD))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = readAccessToken(loginResponseBody);

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.displayName").value("Alice"));
    }

    @Test
    void shouldRejectDuplicateEmailIgnoringCase() throws Exception {
        String email = createUniqueEmail();
        performRegister(new RegisterRequest(email, VALID_PASSWORD, "Alice")).andExpect(status().isCreated());

        performRegister(new RegisterRequest(email.toUpperCase(), VALID_PASSWORD, "Bob"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectWrongPassword() throws Exception {
        String email = createUniqueEmail();
        performRegister(new RegisterRequest(email, VALID_PASSWORD, "Alice")).andExpect(status().isCreated());

        performLogin(new LoginRequest(email, "wrong-password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void shouldRejectInvalidRegisterRequest() throws Exception {
        performRegister(new RegisterRequest("not-an-email", "short", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldReturnUnifiedUnauthorizedResponseWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void shouldReturnUnifiedUnauthorizedResponseWithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private ResultActions performRegister(RegisterRequest registerRequest) throws Exception {
        return mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));
    }

    private ResultActions performLogin(LoginRequest loginRequest) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));
    }

    private String readAccessToken(String responseBody) throws Exception {
        JsonNode responseNode = objectMapper.readTree(responseBody);
        return responseNode.path("data").path("accessToken").asText();
    }
}
