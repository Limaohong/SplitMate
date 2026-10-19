package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.CreateExpenseRequest;
import com.splitmate.dto.ExpenseResponse;
import com.splitmate.service.ExpenseService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /**
     * 新增支出（練習項目）。路徑、參數與回應格式已依 PRACTICE.md 定好，
     * TODO：確認 Service 實作後，這個方法本身不需要再修改。
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> createExpense(@PathVariable Long groupId,
            @Valid @RequestBody CreateExpenseRequest createExpenseRequest, @CurrentUserId Long currentUserId) {
        return ApiResponse.success(expenseService.createExpense(groupId, createExpenseRequest, currentUserId));
    }

    /**
     * 上傳收據（練習項目）。前端以 multipart/form-data 送出，欄位名稱為 "file"。
     * TODO：想想上傳失敗時有哪些情況？每一種應該回什麼 HTTP 狀態碼與 ErrorCode？
     */
    @PostMapping(path = "/{expenseId}/receipt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ExpenseResponse> uploadReceipt(@PathVariable Long groupId, @PathVariable Long expenseId,
            @RequestPart("file") MultipartFile receiptFile, @CurrentUserId Long currentUserId) {
        return ApiResponse.success(expenseService.attachReceipt(groupId, expenseId, receiptFile, currentUserId));
    }
}
