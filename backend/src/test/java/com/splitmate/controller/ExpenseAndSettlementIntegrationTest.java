package com.splitmate.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmate.AbstractIntegrationTest;
import com.splitmate.entity.Expense;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 新增支出 API 是練習項目，這裡直接透過 Repository 寫入測試資料。
 */
class ExpenseAndSettlementIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private ExpenseGroupRepository expenseGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private RegisteredUser alice;
    private RegisteredUser bob;
    private RegisteredUser carol;
    private long groupId;

    @BeforeEach
    void createGroupWithThreeMembers() throws Exception {
        alice = registerUser("Alice");
        bob = registerUser("Bob");
        carol = registerUser("Carol");
        String responseBody = mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"東京五日遊\",\"currencyCode\":\"JPY\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode groupNode = objectMapper.readTree(responseBody).path("data");
        groupId = groupNode.path("id").asLong();
        String inviteCode = groupNode.path("inviteCode").asText();
        for (RegisteredUser invitedUser : new RegisteredUser[] {bob, carol}) {
            mockMvc.perform(post("/api/invitations/{inviteCode}/join", inviteCode)
                            .header(HttpHeaders.AUTHORIZATION, invitedUser.authorization()))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void shouldListExpensesNewestFirstWithShares() throws Exception {
        saveExpense(alice, "900", "晚餐", Map.of(alice, "300", bob, "300", carol, "300"));
        saveExpense(bob, "300", "計程車", Map.of(bob, "150", carol, "150"));

        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, carol.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].description").value("計程車"))
                .andExpect(jsonPath("$.data[0].payerDisplayName").value("Bob"))
                .andExpect(jsonPath("$.data[0].amount").value(300.00))
                .andExpect(jsonPath("$.data[0].shares", hasSize(2)))
                .andExpect(jsonPath("$.data[0].hasReceipt").value(false))
                .andExpect(jsonPath("$.data[1].description").value("晚餐"))
                .andExpect(jsonPath("$.data[1].shares", hasSize(3)));
    }

    @Test
    void shouldCalculateMemberBalancesAndTransfers() throws Exception {
        saveExpense(alice, "900", "晚餐", Map.of(alice, "300", bob, "300", carol, "300"));
        saveExpense(bob, "300", "計程車", Map.of(bob, "150", carol, "150"));

        // Alice：付 900、應付 300 → +600；Bob：付 300、應付 450 → −150；Carol：付 0、應付 450 → −450
        mockMvc.perform(get("/api/groups/{groupId}/settlement", groupId)
                        .header(HttpHeaders.AUTHORIZATION, bob.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currencyCode").value("JPY"))
                .andExpect(jsonPath("$.data.memberBalances", hasSize(3)))
                .andExpect(jsonPath("$.data.memberBalances[0].displayName").value("Alice"))
                .andExpect(jsonPath("$.data.memberBalances[0].netAmount").value(600.00))
                .andExpect(jsonPath("$.data.memberBalances[1].netAmount").value(-150.00))
                .andExpect(jsonPath("$.data.memberBalances[2].paidAmount").value(0.00))
                .andExpect(jsonPath("$.data.memberBalances[2].netAmount").value(-450.00))
                .andExpect(jsonPath("$.data.transfers", hasSize(2)))
                .andExpect(jsonPath("$.data.transfers[0].fromDisplayName").value("Carol"))
                .andExpect(jsonPath("$.data.transfers[0].toDisplayName").value("Alice"))
                .andExpect(jsonPath("$.data.transfers[0].amount").value(450.00))
                .andExpect(jsonPath("$.data.transfers[1].fromDisplayName").value("Bob"))
                .andExpect(jsonPath("$.data.transfers[1].amount").value(150.00));
    }

    @Test
    void shouldReturnZeroBalancesWhenGroupHasNoExpense() throws Exception {
        mockMvc.perform(get("/api/groups/{groupId}/settlement", groupId)
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberBalances", hasSize(3)))
                .andExpect(jsonPath("$.data.memberBalances[0].netAmount").value(0.00))
                .andExpect(jsonPath("$.data.transfers", hasSize(0)));
    }

    @Test
    void shouldReportUnbalancedSharesInsteadOfWrongSettlement() throws Exception {
        // 分攤加總 899 ≠ 支出 900：模擬新增支出邏輯有 bug 時的資料
        saveExpense(alice, "900", "晚餐", Map.of(alice, "300", bob, "300", carol, "299"));

        mockMvc.perform(get("/api/groups/{groupId}/settlement", groupId)
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("SETTLEMENT_UNBALANCED"));
    }

    @Test
    void shouldHideExpensesAndSettlementFromNonMember() throws Exception {
        String outsiderAuthorization = registerUserAndGetAuthorization("Outsider");

        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, outsiderAuthorization))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
        mockMvc.perform(get("/api/groups/{groupId}/settlement", groupId)
                        .header(HttpHeaders.AUTHORIZATION, outsiderAuthorization))
                .andExpect(status().isNotFound());
    }

    private void saveExpense(RegisteredUser payer, String amount, String description,
            Map<RegisteredUser, String> shareAmountByUser) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Expense expense = new Expense(
                    expenseGroupRepository.getReferenceById(groupId),
                    userRepository.getReferenceById(payer.userId()),
                    new BigDecimal(amount),
                    description,
                    userRepository.getReferenceById(payer.userId()));
            shareAmountByUser.forEach((shareUser, shareAmount) ->
                    expense.addShare(userRepository.getReferenceById(shareUser.userId()), new BigDecimal(shareAmount)));
            expenseRepository.save(expense);
        });
    }
}
