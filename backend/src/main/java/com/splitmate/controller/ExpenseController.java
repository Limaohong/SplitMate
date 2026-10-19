package com.splitmate.controller;

import com.splitmate.config.CurrentUserId;
import com.splitmate.dto.ApiResponse;
import com.splitmate.dto.CreateExpenseRequest;
import com.splitmate.dto.ExpensePageResponse;
import com.splitmate.dto.ExpenseResponse;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    // ===================== 第二階段練習項目 =====================

    /**
     * 支出的 cursor 分頁（第二階段練習 11，規格見 practice-guide/11-vue-large-dataset.md）。
     *
     * <p>size 的 @Min / @Max 會觸發 Spring 6.1+ 的「方法參數驗證」，違反時拋出的例外
     * 與 @RequestBody 驗證失敗不同，GlobalExceptionHandler 目前沒有處理它（會變成 500），這也是練習的一部分。
     */
    @GetMapping("/paged")
    public ApiResponse<ExpensePageResponse> getGroupExpensePage(@PathVariable Long groupId,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String cursor,
            @CurrentUserId Long currentUserId) {
        return ApiResponse.success(expenseService.getGroupExpensePage(groupId, size, cursor, currentUserId));
    }

    /**
     * 檢視收據（第二階段練習 16，規格見 practice-guide/16-nginx-protected-files.md）。
     *
     * <p>TODO 練習項目：後端只負責「權限檢查 + 告訴 nginx 要送哪個檔案」，不讀取檔案內容。
     * 回應的 header 與 body 應該長什麼樣子，見規格書與 Phase2AcceptanceTest。
     */
    @GetMapping("/{expenseId}/receipt")
    public ResponseEntity<Void> getReceipt(@PathVariable Long groupId, @PathVariable Long expenseId,
            @CurrentUserId Long currentUserId) {
        // TODO 練習項目：實作完成後移除下面這行
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
