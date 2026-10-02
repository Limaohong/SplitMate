package com.splitmate;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 整合測試共用設定：啟動一個真正的 PostgreSQL 容器（需要本機 Docker 正在執行）。
 * static 容器在同一個 JVM 內只啟動一次，所有繼承的測試類別共用，並搭配 Spring 測試 context 快取。
 */
@SpringBootTest(properties = {
        "app.jwt.secret=integration-test-secret-key-at-least-32-bytes",
        "DB_USERNAME=unused-overridden-by-service-connection",
        "DB_PASSWORD=unused-overridden-by-service-connection"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine");

    static {
        POSTGRES_CONTAINER.start();
    }
}
