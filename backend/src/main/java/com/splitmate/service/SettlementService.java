package com.splitmate.service;

import com.splitmate.dto.MemberAmountTotal;
import com.splitmate.dto.MemberBalanceResponse;
import com.splitmate.dto.SettlementResponse;
import com.splitmate.dto.SettlementTransfer;
import com.splitmate.dto.SettlementTransferResponse;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.GroupMember;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.GroupMemberRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private final GroupService groupService;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementCalculator settlementCalculator;

    public SettlementService(GroupService groupService, GroupMemberRepository groupMemberRepository,
            ExpenseRepository expenseRepository, SettlementCalculator settlementCalculator) {
        this.groupService = groupService;
        this.groupMemberRepository = groupMemberRepository;
        this.expenseRepository = expenseRepository;
        this.settlementCalculator = settlementCalculator;
    }

    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(Long groupId, Long currentUserId) {
        ExpenseGroup group = groupService.getGroupForMember(groupId, currentUserId);
        List<GroupMember> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId);
        Map<Long, BigDecimal> paidAmountByUserId = toAmountByUserId(expenseRepository.sumPaidAmountByGroupId(groupId));
        Map<Long, BigDecimal> owedAmountByUserId = toAmountByUserId(expenseRepository.sumOwedAmountByGroupId(groupId));

        // 沒有任何支出的成員也要列出（金額為 0），讓使用者看到完整名單
        List<MemberBalanceResponse> memberBalances = members.stream()
                .map(member -> buildMemberBalance(member, paidAmountByUserId, owedAmountByUserId))
                .toList();

        Map<Long, BigDecimal> netAmountByUserId = memberBalances.stream()
                .collect(Collectors.toMap(MemberBalanceResponse::userId, MemberBalanceResponse::netAmount,
                        (existingAmount, duplicateAmount) -> existingAmount, LinkedHashMap::new));
        Map<Long, String> displayNameByUserId = memberBalances.stream()
                .collect(Collectors.toMap(MemberBalanceResponse::userId, MemberBalanceResponse::displayName));

        List<SettlementTransferResponse> transfers = settlementCalculator.calculateTransfers(netAmountByUserId)
                .stream()
                .map(transfer -> toTransferResponse(transfer, displayNameByUserId))
                .toList();
        return new SettlementResponse(group.getCurrencyCode(), memberBalances, transfers);
    }

    private MemberBalanceResponse buildMemberBalance(GroupMember member, Map<Long, BigDecimal> paidAmountByUserId,
            Map<Long, BigDecimal> owedAmountByUserId) {
        Long userId = member.getUser().getId();
        BigDecimal paidAmount = paidAmountByUserId.getOrDefault(userId, MoneyRules.ZERO_AMOUNT);
        BigDecimal owedAmount = owedAmountByUserId.getOrDefault(userId, MoneyRules.ZERO_AMOUNT);
        return new MemberBalanceResponse(userId, member.getUser().getDisplayName(), paidAmount, owedAmount,
                MoneyRules.normalizeAmount(paidAmount.subtract(owedAmount)));
    }

    private Map<Long, BigDecimal> toAmountByUserId(List<MemberAmountTotal> memberAmountTotals) {
        return memberAmountTotals.stream()
                .collect(Collectors.toMap(MemberAmountTotal::userId,
                        memberAmountTotal -> MoneyRules.normalizeAmount(memberAmountTotal.totalAmount())));
    }

    private SettlementTransferResponse toTransferResponse(SettlementTransfer transfer,
            Map<Long, String> displayNameByUserId) {
        return new SettlementTransferResponse(
                transfer.fromUserId(), displayNameByUserId.get(transfer.fromUserId()),
                transfer.toUserId(), displayNameByUserId.get(transfer.toUserId()),
                transfer.amount());
    }
}
