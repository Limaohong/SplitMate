package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.UserResponse;
import com.splitmate.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 前端重新整理頁面時，用既有 token 取回目前登入者資料，同時驗證 token 是否仍有效 */
    @GetMapping("/me")
    public ApiResponse<UserResponse> getCurrentUser(@CurrentUserId Long currentUserId) {
        return ApiResponse.success(userService.getUser(currentUserId));
    }
}
