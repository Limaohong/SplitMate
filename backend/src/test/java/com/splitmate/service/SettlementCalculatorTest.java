package com.splitmate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.splitmate.dto.SettlementTransfer;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SettlementCalculatorTest {

    private static final long ALICE_ID = 1L;
    private static final long BOB_ID = 2L;
    private static final long CAROL_ID = 3L;
    private static final long DAVE_ID = 4L;

    private final SettlementCalculator settlementCalculator = new SettlementCalculator();

    @Test
    void shouldLetEveryDebtorPayTheOnlyCreditor() {
        // Alice 付了 300，三人平分：Bob、Carol 各欠 Alice 100
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "200", BOB_ID, "-100", CAROL_ID, "-100"));

        assertThat(transfers).containsExactly(
                new SettlementTransfer(BOB_ID, ALICE_ID, new BigDecimal("100.00")),
                new SettlementTransfer(CAROL_ID, ALICE_ID, new BigDecimal("100.00")));
    }

    @Test
    void shouldReturnNoTransferWhenEveryoneIsSettled() {
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "0", BOB_ID, "0.00"));

        assertThat(transfers).isEmpty();
    }

    @Test
    void shouldSkipIntermediaryInDebtChain() {
        // Alice 欠 Bob 10、Bob 欠 Carol 10 → Bob 淨額為 0，Alice 直接付給 Carol 即可，只需 1 筆
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "-10", BOB_ID, "0", CAROL_ID, "10"));

        assertThat(transfers).containsExactly(new SettlementTransfer(ALICE_ID, CAROL_ID, new BigDecimal("10.00")));
    }

    @Test
    void shouldMatchLargestDebtorWithLargestCreditorFirst() {
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "600", BOB_ID, "-150", CAROL_ID, "-450"));

        assertThat(transfers).containsExactly(
                new SettlementTransfer(CAROL_ID, ALICE_ID, new BigDecimal("450.00")),
                new SettlementTransfer(BOB_ID, ALICE_ID, new BigDecimal("150.00")));
    }

    @Test
    void shouldHandleCentLevelAmountsExactly() {
        // 100 元三人平分常見的 33.34 / 33.33 / 33.33 情境，BigDecimal 不會有 0.1 + 0.2 != 0.3 的浮點誤差
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "66.66", BOB_ID, "-33.33", CAROL_ID, "-33.33"));

        assertThat(transfers).containsExactly(
                new SettlementTransfer(BOB_ID, ALICE_ID, new BigDecimal("33.33")),
                new SettlementTransfer(CAROL_ID, ALICE_ID, new BigDecimal("33.33")));
    }

    @Test
    void shouldBreakTiesByUserIdForDeterministicResult() {
        List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(createNetAmounts(
                DAVE_ID, "50", CAROL_ID, "50", BOB_ID, "-50", ALICE_ID, "-50"));

        assertThat(transfers).containsExactly(
                new SettlementTransfer(ALICE_ID, CAROL_ID, new BigDecimal("50.00")),
                new SettlementTransfer(BOB_ID, DAVE_ID, new BigDecimal("50.00")));
    }

    @Test
    void shouldRejectUnbalancedNetAmounts() {
        assertThatThrownBy(() -> settlementCalculator.calculateTransfers(createNetAmounts(
                ALICE_ID, "100", BOB_ID, "-99.99")))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SETTLEMENT_UNBALANCED));
    }

    /**
     * 性質測試：隨機產生 200 組平衡的淨額，驗證兩個不論輸入為何都必須成立的性質——
     * 1. 套用所有轉帳後每個人都歸零；2. 轉帳筆數不超過「有餘額的人數 − 1」。
     * 固定亂數種子，失敗時可重現。
     */
    @Test
    void shouldAlwaysSettleEveryoneWithinMemberCountMinusOneTransfers() {
        Random random = new Random(20261019L);
        for (int round = 0; round < 200; round++) {
            Map<Long, BigDecimal> netAmountByUserId = createRandomBalancedNetAmounts(random, 2 + random.nextInt(9));

            List<SettlementTransfer> transfers = settlementCalculator.calculateTransfers(netAmountByUserId);

            Map<Long, BigDecimal> remainingAmountByUserId = new HashMap<>(netAmountByUserId);
            for (SettlementTransfer transfer : transfers) {
                assertThat(transfer.amount()).isPositive();
                remainingAmountByUserId.merge(transfer.fromUserId(), transfer.amount(), BigDecimal::add);
                remainingAmountByUserId.merge(transfer.toUserId(), transfer.amount().negate(), BigDecimal::add);
            }
            assertThat(remainingAmountByUserId.values()).allSatisfy(remainingAmount -> assertThat(remainingAmount).isZero());

            long unsettledMemberCount = netAmountByUserId.values().stream()
                    .filter(netAmount -> netAmount.signum() != 0)
                    .count();
            assertThat(transfers.size()).isLessThanOrEqualTo((int) Math.max(0, unsettledMemberCount - 1));
        }
    }

    /** 依序傳入 userId、金額字串成對的參數 */
    private Map<Long, BigDecimal> createNetAmounts(Object... userIdAndAmountPairs) {
        Map<Long, BigDecimal> netAmountByUserId = new LinkedHashMap<>();
        for (int index = 0; index < userIdAndAmountPairs.length; index += 2) {
            netAmountByUserId.put((Long) userIdAndAmountPairs[index],
                    new BigDecimal((String) userIdAndAmountPairs[index + 1]));
        }
        return netAmountByUserId;
    }

    /** 前 n−1 人隨機金額（含小數兩位、可正可負），最後一人補差額，保證總和為 0 */
    private Map<Long, BigDecimal> createRandomBalancedNetAmounts(Random random, int memberCount) {
        Map<Long, BigDecimal> netAmountByUserId = new LinkedHashMap<>();
        BigDecimal runningTotal = BigDecimal.ZERO;
        for (long userId = 1; userId < memberCount; userId++) {
            BigDecimal netAmount = BigDecimal.valueOf(random.nextInt(2_000_001) - 1_000_000, 2);
            netAmountByUserId.put(userId, netAmount);
            runningTotal = runningTotal.add(netAmount);
        }
        netAmountByUserId.put((long) memberCount, runningTotal.negate());
        return netAmountByUserId;
    }
}
