package com.splitmate.service;

import com.splitmate.dto.SettlementTransfer;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.springframework.stereotype.Component;

/**
 * 依每位成員的淨額，計算「誰該付給誰多少錢」。
 *
 * <p>演算法（貪婪法）：每一輪都讓「欠最多的人」付給「被欠最多的人」，金額取兩者較小值，
 * 其中一方歸零後移出，另一方剩餘金額放回佇列，直到所有人歸零。
 *
 * <ul>
 *   <li>每一輪至少讓一個人歸零，因此轉帳筆數最多為「有餘額的人數 − 1」。</li>
 *   <li>這不保證是理論上的最少筆數：找出真正最少筆數等同於「把成員分成最多組、每組淨額剛好為 0」，
 *       屬於 NP-hard 問題。貪婪法在實務上已足夠好，也是一般分帳 App 常見的做法。</li>
 *   <li>同額時依 userId 排序，讓相同輸入永遠得到相同結果，方便測試與使用者對帳。</li>
 * </ul>
 *
 * <p>純計算、不依賴資料庫，方便單元測試。
 */
@Component
public class SettlementCalculator {

    private static final Comparator<MemberBalance> LARGEST_AMOUNT_FIRST = Comparator
            .comparing(MemberBalance::amount, Comparator.reverseOrder())
            .thenComparing(MemberBalance::userId);

    /**
     * @param netAmountByUserId 每位成員的淨額：正數代表別人欠他（應收），負數代表他欠別人（應付）
     * @return 轉帳建議；所有人都已平帳時回傳空清單
     * @throws BusinessException 淨額加總不為 0（帳目不平衡）
     */
    public List<SettlementTransfer> calculateTransfers(Map<Long, BigDecimal> netAmountByUserId) {
        validateBalanced(netAmountByUserId);

        // 兩個佇列都存「正的金額」，讓比較邏輯一致
        PriorityQueue<MemberBalance> creditors = new PriorityQueue<>(LARGEST_AMOUNT_FIRST);
        PriorityQueue<MemberBalance> debtors = new PriorityQueue<>(LARGEST_AMOUNT_FIRST);
        netAmountByUserId.forEach((userId, netAmount) -> {
            BigDecimal normalizedAmount = MoneyRules.normalizeAmount(netAmount);
            if (normalizedAmount.signum() > 0) {
                creditors.add(new MemberBalance(userId, normalizedAmount));
            } else if (normalizedAmount.signum() < 0) {
                debtors.add(new MemberBalance(userId, normalizedAmount.negate()));
            }
        });

        List<SettlementTransfer> transfers = new ArrayList<>();
        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            MemberBalance creditor = creditors.poll();
            MemberBalance debtor = debtors.poll();
            BigDecimal transferAmount = creditor.amount().min(debtor.amount());
            transfers.add(new SettlementTransfer(debtor.userId(), creditor.userId(), transferAmount));
            requeueRemainingBalance(creditors, creditor, transferAmount);
            requeueRemainingBalance(debtors, debtor, transferAmount);
        }
        return transfers;
    }

    private void validateBalanced(Map<Long, BigDecimal> netAmountByUserId) {
        BigDecimal totalNetAmount = netAmountByUserId.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // 用 compareTo 而非 equals：BigDecimal.equals 會比較小數位數，0 與 0.00 會被視為不相等
        if (totalNetAmount.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ErrorCode.SETTLEMENT_UNBALANCED);
        }
    }

    private void requeueRemainingBalance(PriorityQueue<MemberBalance> balances, MemberBalance balance,
            BigDecimal settledAmount) {
        BigDecimal remainingAmount = balance.amount().subtract(settledAmount);
        if (remainingAmount.signum() > 0) {
            balances.add(new MemberBalance(balance.userId(), remainingAmount));
        }
    }

    private record MemberBalance(Long userId, BigDecimal amount) {
    }
}
