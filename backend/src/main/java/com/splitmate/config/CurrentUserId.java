package com.splitmate.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

/**
 * 在 Controller 參數上取得目前登入者的 user ID。
 * principal 是 JWT（{@link org.springframework.security.oauth2.jwt.Jwt}），subject 存的就是 user ID，
 * 用 meta-annotation 包裝後各 Controller 不必重複寫 Long.parseLong(jwt.getSubject())。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "T(java.lang.Long).parseLong(subject)")
public @interface CurrentUserId {
}
