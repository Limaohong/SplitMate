package com.splitmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 對應 application.yml 的 app.receipt.*。
 *
 * @param storageDirectory 收據圖片存放目錄（容器內為 /app/data/receipts，由 docker volume 保存）
 */
@ConfigurationProperties(prefix = "app.receipt")
public record ReceiptStorageProperties(String storageDirectory) {
}
