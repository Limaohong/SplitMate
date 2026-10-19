package com.splitmate.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金額計算的共用規則，與資料庫 NUMERIC(12,2) 一致。所有金額運算都應透過這裡統一小數位數與捨入方式。
 */
public final class MoneyRules {

    public static final int MONEY_SCALE = 2;
    /** 四捨五入。明確指定，避免 BigDecimal 除法在除不盡時拋出 ArithmeticException */
    public static final RoundingMode MONEY_ROUNDING_MODE = RoundingMode.HALF_UP;
    public static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);

    private MoneyRules() {
    }

    /** 統一成兩位小數；例如 100 → 100.00，避免 100 與 100.00 在 equals 比較時被視為不同 */
    public static BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }
}
