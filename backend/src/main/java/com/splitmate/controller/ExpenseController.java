package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.ExpenseResponse;
import com.splitmate.service.ExpenseService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public ApiResponse<List<ExpenseResponse>> getGroupExpenses(@PathVariable Long groupId,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(expenseService.getGroupExpenses(groupId, currentUserId));
    }
}
