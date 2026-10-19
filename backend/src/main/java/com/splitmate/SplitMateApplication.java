package com.splitmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// 自動註冊所有 @ConfigurationProperties（JwtProperties、ReceiptStorageProperties），不必逐一列在各 Config 類別
@ConfigurationPropertiesScan
public class SplitMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(SplitMateApplication.class, args);
    }
}
