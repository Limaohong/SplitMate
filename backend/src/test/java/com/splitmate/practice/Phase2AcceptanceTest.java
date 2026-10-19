package com.splitmate.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmate.AbstractIntegrationTest;
import com.splitmate.entity.Expense;
import com.splitmate.repository.ExpenseGroupRepository;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 第二階段練習的後端驗收測試：
 * <ul>
 *   <li>練習 11：支出的 cursor 分頁 GET /api/groups/{groupId}/expenses/paged</li>
 *   <li>練習 16：收據檢視 GET /api/groups/{groupId}/expenses/{expenseId}/receipt（交由 nginx 以 X-Accel-Redirect 傳檔）</li>
 * </ul>
 * 測試資料直接透過 Repository 寫入，不依賴第一階段的新增支出、上傳收據是否已完成。
 *
 * <p>使用方式同 PracticeAcceptanceTest：實作後移除 {@code @Disabled}，或用 JUnit 的 deactivate 參數只跑指定方法。
 */
@Disabled("第二階段練習：實作 cursor 分頁（11）與收據檢視（16）後移除這行")
class Phase2AcceptanceTest extends AbstractIntegrationTest {

    private static final String RECEIPT_INTERNAL_PATH_PREFIX = "/internal/receipts/";

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
    private long groupId;

    @BeforeEach
    void createGroupWithTwoMembers() throws Exception {
        alice = registerUser("Alice");
        bob = registerUser("Bob");
        groupId = createGroup(alice);
        joinGroup(groupId, bob);
    }

    // ================= 練習 11：cursor 分頁 =================

    @Test
    void shouldPageThroughExpensesNewestFirstWithoutGapsOrDuplicates() throws Exception {
        List<Long> insertedExpenseIds = saveExpenses(groupId, 7);
        List<Long> expectedIdsNewestFirst = new ArrayList<>(insertedExpenseIds);
        Collections.reverse(expectedIdsNewestFirst);

        List<Long> pagedIds = new ArrayList<>();
        List<Integer> pageSizes = new ArrayList<>();
        String cursor = null;
        do {
            JsonNode pageNode = fetchPage(alice, 3, cursor);
            pageNode.path("items").forEach(itemNode -> {
                pagedIds.add(itemNode.path("id").asLong());
                assertThat(itemNode.path("shares")).as("每筆支出都要包含分攤明細").hasSize(2);
            });
            pageSizes.add(pageNode.path("items").size());
            cursor = pageNode.path("nextCursor").isNull() ? null : pageNode.path("nextCursor").asText();
        } while (cursor != null);

        assertThat(pageSizes).containsExactly(3, 3, 1);
        assertThat(pagedIds).containsExactlyElementsOf(expectedIdsNewestFirst);
    }

    @Test
    void shouldKeepCursorStableWhenNewExpenseIsAddedBetweenPages() throws Exception {
        saveExpenses(groupId, 6);
        JsonNode firstPage = fetchPage(alice, 3, null);
        Set<Long> firstPageIds = collectIds(firstPage);

        // 使用者捲動到一半時，有人新增了一筆支出
        long newestExpenseId = saveExpenses(groupId, 1).get(0);
        JsonNode secondPage = fetchPage(alice, 3, firstPage.path("nextCursor").asText());
        Set<Long> secondPageIds = collectIds(secondPage);

        assertThat(secondPageIds).hasSize(3);
        assertThat(secondPageIds).as("第二頁不可重複出現第一頁的資料").doesNotContainAnyElementsOf(firstPageIds);
        assertThat(secondPageIds).as("比 cursor 新的資料不應出現在後面的頁").doesNotContain(newestExpenseId);
        assertThat(secondPage.path("nextCursor").isNull()).isTrue();
    }

    @Test
    void shouldUseDefaultPageSizeOfFifty() throws Exception {
        saveExpenses(groupId, 55);

        JsonNode firstPage = fetchPage(alice, null, null);

        assertThat(firstPage.path("items")).hasSize(50);
        assertThat(firstPage.path("nextCursor").isNull()).isFalse();
    }

    @Test
    void shouldRejectInvalidPageSize() throws Exception {
        performFetchPage(alice, "0", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        performFetchPage(alice, "101", null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void shouldRejectMalformedCursor() throws Exception {
        saveExpenses(groupId, 2);

        performFetchPage(alice, "10", "not-a-valid-cursor")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURSOR"));
    }

    @Test
    void shouldHidePagedExpensesFromNonMember() throws Exception {
        RegisteredUser outsider = registerUser("Outsider");

        performFetchPage(outsider, "10", null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }

    // ================= 練習 16：收據檢視（X-Accel-Redirect） =================

    @Test
    void shouldDelegateReceiptDeliveryToNginx() throws Exception {
        long pngExpenseId = saveExpenseWithReceipt(groupId, "rcpt-png-test.png");
        long jpegExpenseId = saveExpenseWithReceipt(groupId, "rcpt-jpg-test.jpg");

        mockMvc.perform(get("/api/groups/{groupId}/expenses/{expenseId}/receipt", groupId, pngExpenseId)
                        .header(HttpHeaders.AUTHORIZATION, bob.authorization()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Redirect", RECEIPT_INTERNAL_PATH_PREFIX + "rcpt-png-test.png"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE))
                .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray())
                        .as("後端不應回傳檔案內容，由 nginx 負責").isEmpty());
        mockMvc.perform(get("/api/groups/{groupId}/expenses/{expenseId}/receipt", groupId, jpegExpenseId)
                        .header(HttpHeaders.AUTHORIZATION, bob.authorization()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    void shouldReturnNotFoundWhenExpenseHasNoReceipt() throws Exception {
        long expenseId = saveExpenses(groupId, 1).get(0);

        mockMvc.perform(get("/api/groups/{groupId}/expenses/{expenseId}/receipt", groupId, expenseId)
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RECEIPT_NOT_FOUND"));
    }

    @Test
    void shouldNotServeReceiptOutsideMemberGroup() throws Exception {
        long expenseId = saveExpenseWithReceipt(groupId, "rcpt-secret.png");
        RegisteredUser outsider = registerUser("Outsider");
        long outsiderGroupId = createGroup(outsider);

        // 非成員直接存取
        mockMvc.perform(get("/api/groups/{groupId}/expenses/{expenseId}/receipt", groupId, expenseId)
                        .header(HttpHeaders.AUTHORIZATION, outsider.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"))
                .andExpect(header().doesNotExist("X-Accel-Redirect"));
        // 用自己群組的路徑存取別人群組的支出（IDOR）
        mockMvc.perform(get("/api/groups/{groupId}/expenses/{expenseId}/receipt", outsiderGroupId, expenseId)
                        .header(HttpHeaders.AUTHORIZATION, outsider.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPENSE_NOT_FOUND"))
                .andExpect(header().doesNotExist("X-Accel-Redirect"));
    }

    // ================= 輔助方法 =================

    private JsonNode fetchPage(RegisteredUser user, Integer pageSize, String cursor) throws Exception {
        String responseBody = performFetchPage(user, pageSize == null ? null : String.valueOf(pageSize), cursor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data");
    }

    private ResultActions performFetchPage(RegisteredUser user, String pageSize, String cursor) throws Exception {
        var requestBuilder = get("/api/groups/{groupId}/expenses/paged", groupId)
                .header(HttpHeaders.AUTHORIZATION, user.authorization());
        if (pageSize != null) {
            requestBuilder.param("size", pageSize);
        }
        if (cursor != null) {
            requestBuilder.param("cursor", cursor);
        }
        return mockMvc.perform(requestBuilder);
    }

    private Set<Long> collectIds(JsonNode pageNode) {
        Set<Long> expenseIds = new HashSet<>();
        pageNode.path("items").forEach(itemNode -> expenseIds.add(itemNode.path("id").asLong()));
        return expenseIds;
    }

    /** 依序建立支出（越後面建立的越新），回傳建立順序的 ID */
    private List<Long> saveExpenses(long targetGroupId, int expenseCount) {
        List<Long> expenseIds = new ArrayList<>();
        for (int expenseIndex = 0; expenseIndex < expenseCount; expenseIndex++) {
            expenseIds.add(saveExpense(targetGroupId, "支出 " + expenseIndex, null));
        }
        return expenseIds;
    }

    private long saveExpenseWithReceipt(long targetGroupId, String receiptPath) {
        return saveExpense(targetGroupId, "有收據的支出", receiptPath);
    }

    private long saveExpense(long targetGroupId, String description, String receiptPath) {
        return transactionTemplate.execute(transactionStatus -> {
            Expense expense = new Expense(
                    expenseGroupRepository.getReferenceById(targetGroupId),
                    userRepository.getReferenceById(alice.userId()),
                    new BigDecimal("200"),
                    description,
                    userRepository.getReferenceById(alice.userId()));
            expense.addShare(userRepository.getReferenceById(alice.userId()), new BigDecimal("100"));
            expense.addShare(userRepository.getReferenceById(bob.userId()), new BigDecimal("100"));
            if (receiptPath != null) {
                expense.attachReceipt(receiptPath);
            }
            return expenseRepository.saveAndFlush(expense).getId();
        });
    }

    private long createGroup(RegisteredUser owner) throws Exception {
        String responseBody = mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, owner.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"第二階段練習群組\",\"currencyCode\":\"TWD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data").path("id").asLong();
    }

    private void joinGroup(long targetGroupId, RegisteredUser invitedUser) throws Exception {
        String groupBody = mockMvc.perform(get("/api/groups/{groupId}", targetGroupId)
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization()))
                .andExpect(jsonPath("$.data.members", hasSize(1)))
                .andReturn().getResponse().getContentAsString();
        String inviteCode = objectMapper.readTree(groupBody).path("data").path("inviteCode").asText();
        mockMvc.perform(post("/api/invitations/{inviteCode}/join", inviteCode)
                        .header(HttpHeaders.AUTHORIZATION, invitedUser.authorization()))
                .andExpect(status().isOk());
    }
}
