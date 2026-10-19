package com.splitmate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitmate.dto.RegisterRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 整合測試共用設定：啟動一個真正的 PostgreSQL 容器（需要本機 Docker 正在執行）。
 * static 容器在同一個 JVM 內只啟動一次，所有繼承的測試類別共用，並搭配 Spring 測試 context 快取。
 * RANDOM_PORT：啟動真正的 HTTP server，WebSocket 測試才能連線；MockMvc 測試同樣可用。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=integration-test-secret-key-at-least-32-bytes",
        "DB_USERNAME=unused-overridden-by-service-connection",
        "DB_PASSWORD=unused-overridden-by-service-connection",
        "app.receipt.storage-directory=target/test-receipts"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String VALID_PASSWORD = "password123";

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES_CONTAINER.start();
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /** 每次產生不同 email，測試之間共用同一個 DB 也不會互相干擾 */
    protected String createUniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    /** 註冊一位新使用者並回傳 Authorization header 值（"Bearer xxx"） */
    protected String registerUserAndGetAuthorization(String displayName) throws Exception {
        return registerUser(displayName).authorization();
    }

    protected RegisteredUser registerUser(String displayName) throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(createUniqueEmail(), VALID_PASSWORD, displayName);
        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode authNode = objectMapper.readTree(responseBody).path("data");
        return new RegisteredUser("Bearer " + authNode.path("accessToken").asText(),
                authNode.path("user").path("id").asLong());
    }

    /** 測試用的已註冊使用者：authorization 為 "Bearer xxx" */
    protected record RegisteredUser(String authorization, Long userId) {
    }
}
