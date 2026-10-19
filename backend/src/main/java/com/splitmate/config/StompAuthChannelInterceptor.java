package com.splitmate.config;

import com.splitmate.repository.GroupMemberRepository;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * STOMP 連線的身分驗證與訂閱權限檢查：
 * <ul>
 *   <li>CONNECT：從 STOMP header 的 Authorization 讀取 JWT 並驗證，通過後把 userId 綁定到這條連線</li>
 *   <li>SUBSCRIBE：只允許訂閱「自己是成員」的群組支出頻道，避免外人監聽別的群組</li>
 * </ul>
 * 拋出例外時，Spring 會回傳 STOMP ERROR frame 給前端並關閉連線。
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final GroupMemberRepository groupMemberRepository;

    public StompAuthChannelInterceptor(JwtDecoder jwtDecoder, GroupMemberRepository groupMemberRepository) {
        this.jwtDecoder = jwtDecoder;
        this.groupMemberRepository = groupMemberRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateConnection(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }
        return message;
    }

    private void authenticateConnection(StompHeaderAccessor accessor) {
        String authorizationHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new MessageDeliveryException("缺少 Authorization header（格式：Bearer <token>）");
        }
        try {
            Jwt jwt = jwtDecoder.decode(authorizationHeader.substring(BEARER_PREFIX.length()));
            // principal 名稱存 userId；Spring 會把它與這條 WebSocket session 綁定，後續 frame 都能取得
            accessor.setUser(UsernamePasswordAuthenticationToken.authenticated(jwt.getSubject(), null, List.of()));
        } catch (JwtException exception) {
            throw new MessageDeliveryException("Token 無效或已過期");
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (user == null) {
            throw new MessageDeliveryException("尚未通過身分驗證");
        }
        Long groupId = WebSocketDestinations.parseGroupIdFromExpensesTopic(accessor.getDestination())
                .orElseThrow(() -> new MessageDeliveryException("不允許訂閱此頻道：" + accessor.getDestination()));
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, Long.valueOf(user.getName()))) {
            throw new MessageDeliveryException("你不是此群組的成員");
        }
    }
}
