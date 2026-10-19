package com.splitmate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.splitmate.AbstractIntegrationTest;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * 驗證 WebSocket 基礎設施（已完成的部分）：JWT 驗證與訂閱權限。推播本身是練習項目。
 */
class WebSocketIntegrationTest extends AbstractIntegrationTest {

    private static final long TIMEOUT_SECONDS = 5;

    @LocalServerPort
    private int serverPort;

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void createStompClient() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void stopStompClient() {
        stompClient.stop();
    }

    @Test
    void shouldDeliverMessageToSubscribedGroupMember() throws Exception {
        RegisteredUser alice = registerUser("Alice");
        long groupId = createGroup(alice.authorization());
        StompSession stompSession = connect(alice.authorization(), new ErrorRecordingSessionHandler())
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        BlockingQueue<Map<?, ?>> receivedPayloads = new LinkedBlockingQueue<>();
        String groupTopic = WebSocketDestinations.getGroupExpensesTopic(groupId);
        stompSession.subscribe(groupTopic, createPayloadCollector(receivedPayloads));

        // SUBSCRIBE 是非同步處理的，重送到收到為止，避免「訂閱還沒生效就送出」造成測試不穩定
        Map<?, ?> receivedPayload = null;
        for (int attempt = 0; attempt < 20 && receivedPayload == null; attempt++) {
            simpMessagingTemplate.convertAndSend(groupTopic, Map.of("groupId", groupId));
            receivedPayload = receivedPayloads.poll(250, TimeUnit.MILLISECONDS);
        }

        assertThat(receivedPayload).isNotNull();
        assertThat(((Number) receivedPayload.get("groupId")).longValue()).isEqualTo(groupId);
    }

    @Test
    void shouldRejectSubscriptionToOtherGroup() throws Exception {
        RegisteredUser alice = registerUser("Alice");
        RegisteredUser outsider = registerUser("Outsider");
        long groupId = createGroup(alice.authorization());
        ErrorRecordingSessionHandler sessionHandler = new ErrorRecordingSessionHandler();
        StompSession stompSession = connect(outsider.authorization(), sessionHandler)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        stompSession.subscribe(WebSocketDestinations.getGroupExpensesTopic(groupId),
                createPayloadCollector(new LinkedBlockingQueue<>()));

        assertThat(sessionHandler.errorMessages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("不是此群組的成員");
    }

    @Test
    void shouldRejectConnectionWithoutToken() throws Exception {
        ErrorRecordingSessionHandler sessionHandler = new ErrorRecordingSessionHandler();

        connect(null, sessionHandler);

        assertThat(sessionHandler.errorMessages.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS)).contains("Authorization");
    }

    private CompletableFuture<StompSession> connect(String authorization, StompSessionHandlerAdapter sessionHandler) {
        StompHeaders connectHeaders = new StompHeaders();
        if (authorization != null) {
            connectHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
        }
        return stompClient.connectAsync("ws://localhost:" + serverPort + WebSocketDestinations.ENDPOINT_PATH,
                new WebSocketHttpHeaders(), connectHeaders, sessionHandler);
    }

    private long createGroup(String authorization) throws Exception {
        String responseBody = mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"測試群組\",\"currencyCode\":\"TWD\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("data").path("id").asLong();
    }

    private StompFrameHandler createPayloadCollector(BlockingQueue<Map<?, ?>> receivedPayloads) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedPayloads.add((Map<?, ?>) payload);
            }
        };
    }

    /** 收集伺服器回傳的 STOMP ERROR frame 訊息 */
    private static class ErrorRecordingSessionHandler extends StompSessionHandlerAdapter {

        private final BlockingQueue<String> errorMessages = new LinkedBlockingQueue<>();

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            errorMessages.add(String.valueOf(headers.getFirst("message")));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers,
                byte[] payload, Throwable exception) {
            errorMessages.add(String.valueOf(exception.getMessage()));
        }
    }
}
