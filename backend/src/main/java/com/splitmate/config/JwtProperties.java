package com.splitmate.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 對應 application.yml 的 app.jwt.*。
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {

    /** HS256 要求金鑰至少 256 bits（32 bytes），太短 Nimbus 會在第一次簽發時才拋錯，因此在啟動時先檢查 */
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret（環境變數 JWT_SECRET）至少需要 " + MIN_SECRET_BYTES + " bytes");
        }
        if (accessTokenTtl == null || accessTokenTtl.isNegative() || accessTokenTtl.isZero()) {
            throw new IllegalStateException("app.jwt.access-token-ttl 必須大於 0");
        }
    }
}
