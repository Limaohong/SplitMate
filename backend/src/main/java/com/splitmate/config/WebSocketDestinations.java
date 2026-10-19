package com.splitmate.config;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 目的地（destination）集中定義，推播端與訂閱權限檢查共用同一份格式，避免兩邊字串不一致。
 */
public final class WebSocketDestinations {

    public static final String ENDPOINT_PATH = "/ws";
    public static final String BROKER_PREFIX = "/topic";
    public static final String APPLICATION_PREFIX = "/app";

    private static final String GROUP_EXPENSES_TOPIC_FORMAT = BROKER_PREFIX + "/groups/%d/expenses";
    private static final Pattern GROUP_EXPENSES_TOPIC_PATTERN = Pattern.compile("^/topic/groups/(\\d+)/expenses$");

    private WebSocketDestinations() {
    }

    /** 某群組的支出通知頻道，例如 /topic/groups/12/expenses */
    public static String getGroupExpensesTopic(Long groupId) {
        return String.format(GROUP_EXPENSES_TOPIC_FORMAT, groupId);
    }

    /** 從訂閱目的地解析出群組 ID；不是群組支出頻道時回傳 empty */
    public static Optional<Long> parseGroupIdFromExpensesTopic(String destination) {
        if (destination == null) {
            return Optional.empty();
        }
        Matcher matcher = GROUP_EXPENSES_TOPIC_PATTERN.matcher(destination);
        return matcher.matches() ? Optional.of(Long.valueOf(matcher.group(1))) : Optional.empty();
    }
}
