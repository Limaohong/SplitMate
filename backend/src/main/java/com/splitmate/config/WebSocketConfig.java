package com.splitmate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(WebSocketDestinations.ENDPOINT_PATH)
                // 允許任何來源：驗證靠 STOMP CONNECT 時帶的 JWT，而不是瀏覽器自動夾帶的 cookie，
                // 因此不會有跨站 WebSocket 劫持（CSWSH）的風險；也避免 nginx 改變 port 時 Origin 檢查失敗
                .setAllowedOriginPatterns("*");
        // 刻意不使用 SockJS：SockJS 會在 WebSocket 失敗時自動退回 HTTP 輪詢，
        // 會把 nginx 沒有正確代理 WebSocket 的問題藏起來（這正是練習項目之一）
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 單機內建 broker 即可；若後端要多台水平擴充，需改用 RabbitMQ 等外部 broker 才能跨節點推播
        registry.enableSimpleBroker(WebSocketDestinations.BROKER_PREFIX);
        registry.setApplicationDestinationPrefixes(WebSocketDestinations.APPLICATION_PREFIX);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
