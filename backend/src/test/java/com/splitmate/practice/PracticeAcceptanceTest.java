package com.splitmate.practice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.splitmate.AbstractIntegrationTest;
import com.splitmate.config.WebSocketDestinations;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * 練習項目的驗收測試，對應 PRACTICE.md 的「驗收條件」。
 *
 * <p>使用方式：實作完成後移除 class 上的 {@code @Disabled}，執行 {@code ./mvnw test}，全部通過即代表後端符合規格。
 * 也可以一次只移除單一測試方法上的註解、逐步完成。測試只檢查「行為」，不限制你的實作方式。
 */
@Disabled("練習項目：實作新增支出、收據上傳、即時通知後移除這行")
class PracticeAcceptanceTest extends AbstractIntegrationTest {

    /** 最小合法 PNG 檔頭（8 bytes 簽章）＋少量內容，足以通過「檢查檔案內容是否為圖片」 */
    private static final byte[] PNG_FILE_BYTES = {
            (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'};

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestRestTemplate testRestTemplate;

    private RegisteredUser alice;
    private RegisteredUser bob;
    private RegisteredUser carol;
    private long groupId;

    @BeforeEach
    void createGroupWithThreeMembers() throws Exception {
        alice = registerUser("Alice");
        bob = registerUser("Bob");
        carol = registerUser("Carol");
        groupId = createGroupAndInvite(alice, bob, carol);
    }

    // ================= 後端 1：新增支出 API =================

    @Test
    void shouldCreateExpenseAndReflectInListAndSettlement() throws Exception {
        String requestBody = buildExpenseJson("1000", "電費", alice.userId(),
                share(alice.userId(), "333.34"), share(bob.userId(), "333.33"), share(carol.userId(), "333.33"));

        performCreateExpense(bob.authorization(), requestBody)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.description").value("電費"))
                .andExpect(jsonPath("$.data.amount").value(1000.00))
                .andExpect(jsonPath("$.data.payerDisplayName").value("Alice"))
                .andExpect(jsonPath("$.data.shares", hasSize(3)))
                .andExpect(jsonPath("$.data.hasReceipt").value(false));

        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, carol.authorization()))
                .andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get("/api/groups/{groupId}/settlement", groupId)
                        .header(HttpHeaders.AUTHORIZATION, carol.authorization()))
                .andExpect(jsonPath("$.data.memberBalances[0].netAmount").value(666.66))
                .andExpect(jsonPath("$.data.transfers", hasSize(2)));
    }

    @Test
    void shouldRejectSharesNotSummingToAmount() throws Exception {
        performCreateExpense(alice.authorization(), buildExpenseJson("1000", "電費", alice.userId(),
                share(alice.userId(), "333.33"), share(bob.userId(), "333.33"), share(carol.userId(), "333.33")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPENSE_SHARE_SUM_MISMATCH"));
    }

    @Test
    void shouldRejectPayerOrShareMemberOutsideGroup() throws Exception {
        RegisteredUser outsider = registerUser("Outsider");

        performCreateExpense(alice.authorization(), buildExpenseJson("100", "午餐", outsider.userId(),
                share(alice.userId(), "100")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPENSE_MEMBER_INVALID"));
        performCreateExpense(alice.authorization(), buildExpenseJson("100", "午餐", alice.userId(),
                share(alice.userId(), "50"), share(outsider.userId(), "50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPENSE_MEMBER_INVALID"));
    }

    @Test
    void shouldRejectDuplicatedShareMember() throws Exception {
        performCreateExpense(alice.authorization(), buildExpenseJson("100", "午餐", alice.userId(),
                share(bob.userId(), "50"), share(bob.userId(), "50")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("EXPENSE_SHARE_DUPLICATED"));
    }

    @Test
    void shouldHideGroupFromNonMemberWhenCreatingExpense() throws Exception {
        RegisteredUser outsider = registerUser("Outsider");

        performCreateExpense(outsider.authorization(), buildExpenseJson("100", "午餐", alice.userId(),
                share(alice.userId(), "100")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }

    @Test
    void shouldValidateRequestFormat() throws Exception {
        performCreateExpense(alice.authorization(), buildExpenseJson("0", "午餐", alice.userId(),
                share(alice.userId(), "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        performCreateExpense(alice.authorization(), buildExpenseJson("10.001", "午餐", alice.userId(),
                share(alice.userId(), "10.001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ================= 後端 2：收據上傳 API =================

    @Test
    void shouldUploadImageReceipt() throws Exception {
        long expenseId = createExpense(alice);

        performUploadReceipt(expenseId, new MockMultipartFile("file", "receipt.png", "image/png", PNG_FILE_BYTES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(expenseId))
                .andExpect(jsonPath("$.data.hasReceipt").value(true));
        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, alice.authorization()))
                .andExpect(jsonPath("$.data[0].hasReceipt").value(true));
    }

    @Test
    void shouldRejectEmptyOrNonImageReceipt() throws Exception {
        long expenseId = createExpense(alice);

        performUploadReceipt(expenseId, new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECEIPT_FILE_INVALID"));
        // 副檔名與 Content-Type 都宣稱是 PNG，但內容其實是文字：必須檢查檔案內容本身
        performUploadReceipt(expenseId, new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("RECEIPT_FILE_INVALID"));
    }

    @Test
    void shouldRejectReceiptForExpenseInAnotherGroup() throws Exception {
        long expenseId = createExpense(alice);
        RegisteredUser outsider = registerUser("Outsider");
        long outsiderGroupId = createGroupAndInvite(outsider);

        // outsider 是自己群組的成員，但這筆支出不屬於那個群組
        mockMvc.perform(multipart("/api/groups/{groupId}/expenses/{expenseId}/receipt", outsiderGroupId, expenseId)
                        .file(new MockMultipartFile("file", "receipt.png", "image/png", PNG_FILE_BYTES))
                        .header(HttpHeaders.AUTHORIZATION, outsider.authorization()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("EXPENSE_NOT_FOUND"));
    }

    /** MockMvc 不會套用 multipart 大小限制，因此用真正的 HTTP 請求測試 */
    @Test
    void shouldRejectReceiptLargerThanFiveMegabytes() throws Exception {
        long expenseId = createExpense(alice);
        byte[] oversizedFileBytes = new byte[5 * 1024 * 1024 + 100 * 1024];
        System.arraycopy(PNG_FILE_BYTES, 0, oversizedFileBytes, 0, PNG_FILE_BYTES.length);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        requestHeaders.set(HttpHeaders.AUTHORIZATION, alice.authorization());
        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("file", new ByteArrayResource(oversizedFileBytes) {
            @Override
            public String getFilename() {
                return "large.png";
            }
        });

        ResponseEntity<String> response = testRestTemplate.postForEntity(
                "/api/groups/{groupId}/expenses/{expenseId}/receipt",
                new HttpEntity<>(multipartBody, requestHeaders), String.class, groupId, expenseId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(objectMapper.readTree(response.getBody()).path("code").asText()).isEqualTo("RECEIPT_TOO_LARGE");
    }

    // ================= 後端 3：即時通知 =================

    @Test
    void shouldNotifySubscribedMembersAfterExpenseCreated() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        try {
            StompHeaders connectHeaders = new StompHeaders();
            connectHeaders.add(HttpHeaders.AUTHORIZATION, carol.authorization());
            StompSession stompSession = stompClient.connectAsync(
                            "ws://localhost:" + serverPort + WebSocketDestinations.ENDPOINT_PATH,
                            new WebSocketHttpHeaders(), connectHeaders, new StompSessionHandlerAdapter() { })
                    .get(5, TimeUnit.SECONDS);
            BlockingQueue<Map<?, ?>> receivedMessages = new LinkedBlockingQueue<>();
            stompSession.subscribe(WebSocketDestinations.getGroupExpensesTopic(groupId), new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    receivedMessages.add((Map<?, ?>) payload);
                }
            });
            // 給伺服器一點時間處理 SUBSCRIBE（非同步），再觸發新增支出
            TimeUnit.MILLISECONDS.sleep(500);

            createExpense(alice);

            Map<?, ?> expenseCreatedMessage = receivedMessages.poll(5, TimeUnit.SECONDS);
            assertThat(expenseCreatedMessage).as("Carol 應在 5 秒內收到 ExpenseCreatedMessage").isNotNull();
            assertThat(((Number) expenseCreatedMessage.get("groupId")).longValue()).isEqualTo(groupId);
            assertThat(((Map<?, ?>) expenseCreatedMessage.get("expense")).get("description")).isEqualTo("午餐");
        } finally {
            stompClient.stop();
        }
    }

    // ================= 輔助方法 =================

    private long createGroupAndInvite(RegisteredUser owner, RegisteredUser... invitedUsers) throws Exception {
        String responseBody = mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, owner.authorization())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"練習群組\",\"currencyCode\":\"TWD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode groupNode = objectMapper.readTree(responseBody).path("data");
        for (RegisteredUser invitedUser : invitedUsers) {
            mockMvc.perform(post("/api/invitations/{inviteCode}/join", groupNode.path("inviteCode").asText())
                            .header(HttpHeaders.AUTHORIZATION, invitedUser.authorization()))
                    .andExpect(status().isOk());
        }
        return groupNode.path("id").asLong();
    }

    private long createExpense(RegisteredUser payer) throws Exception {
        String responseBody = performCreateExpense(payer.authorization(), buildExpenseJson("300", "午餐",
                payer.userId(), share(alice.userId(), "100"), share(bob.userId(), "100"), share(carol.userId(), "100")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data").path("id").asLong();
    }

    private ResultActions performCreateExpense(String authorization, String requestBody) throws Exception {
        return mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    private ResultActions performUploadReceipt(long expenseId, MockMultipartFile receiptFile) throws Exception {
        return mockMvc.perform(multipart("/api/groups/{groupId}/expenses/{expenseId}/receipt", groupId, expenseId)
                .file(receiptFile)
                .header(HttpHeaders.AUTHORIZATION, alice.authorization()));
    }

    private String share(Long userId, String shareAmount) {
        return "{\"userId\":" + userId + ",\"shareAmount\":" + shareAmount + "}";
    }

    private String buildExpenseJson(String amount, String description, Long payerUserId, String... shareJsons) {
        return "{\"amount\":" + amount + ",\"description\":\"" + description + "\",\"payerUserId\":" + payerUserId
                + ",\"shares\":[" + String.join(",", shareJsons) + "]}";
    }
}
