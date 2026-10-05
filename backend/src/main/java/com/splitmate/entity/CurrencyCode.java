package com.splitmate.entity;

/**
 * 群組支援的幣別（ISO 4217）。用 enum 限制可選值，避免任意字串寫入資料庫。
 */
public enum CurrencyCode {
    TWD,
    JPY,
    USD,
    EUR,
    KRW,
    HKD
}
