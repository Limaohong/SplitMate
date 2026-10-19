package com.splitmate.service;

import com.splitmate.dto.CreateExpenseRequest;
import com.splitmate.dto.ExpensePageResponse;
import com.splitmate.dto.ExpenseResponse;
import com.splitmate.exception.BusinessException;
import com.splitmate.exception.ErrorCode;
import com.splitmate.repository.ExpenseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final GroupService groupService;

    public ExpenseService(ExpenseRepository expenseRepository, GroupService groupService) {
        this.expenseRepository = expenseRepository;
        this.groupService = groupService;
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getGroupExpenses(Long groupId, Long currentUserId) {
        groupService.getGroupForMember(groupId, currentUserId);
        return expenseRepository.findAllWithSharesByGroupId(groupId).stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    /**
     * 新增支出（練習項目，規格見 PRACTICE.md「後端 1：新增支出 API」）。
     *
     * <p>TODO 實作步驟提示：
     * <ol>
     *   <li>確認目前使用者是群組成員（可重用 groupService.getGroupForMember）</li>
     *   <li>驗證付款人、每位分攤成員都屬於這個群組，且分攤成員沒有重複</li>
     *   <li>驗證分攤金額加總「剛好」等於支出金額（比較 BigDecimal 要用哪個方法？）</li>
     *   <li>建立 Expense，透過 expense.addShare(...) 加入分攤後儲存</li>
     *   <li>支出建立後通知群組成員（ExpenseNotificationService），想想通知應該在交易提交「前」還是「後」送出</li>
     *   <li>回傳 ExpenseResponse（注意 LAZY 關聯要在交易內讀取）</li>
     * </ol>
     */
    @Transactional
    public ExpenseResponse createExpense(Long groupId, CreateExpenseRequest createExpenseRequest, Long currentUserId) {
        // TODO 練習項目：實作完成後移除下面這行
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    /**
     * 上傳收據並綁定到支出（練習項目，規格見 PRACTICE.md「後端 2：收據上傳 API」）。
     *
     * <p>TODO 實作步驟提示：
     * <ol>
     *   <li>確認目前使用者是群組成員，且這筆支出屬於該群組（不能用 A 群組的路徑改 B 群組的支出）</li>
     *   <li>驗證檔案：不可為空、只接受圖片格式（不要只相信副檔名或前端傳來的 Content-Type）</li>
     *   <li>透過 ReceiptStorageService 存檔，取得相對路徑</li>
     *   <li>expense.attachReceipt(相對路徑)，回傳更新後的 ExpenseResponse</li>
     * </ol>
     */
    @Transactional
    public ExpenseResponse attachReceipt(Long groupId, Long expenseId, MultipartFile receiptFile, Long currentUserId) {
        // TODO 練習項目：實作完成後移除下面這行
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }

    /**
     * 支出的 cursor 分頁（第二階段練習 11，規格見 practice-guide/11-vue-large-dataset.md）。
     *
     * <p>TODO 實作提示：
     * <ol>
     *   <li>cursor 需要能唯一定位「上一頁最後一筆」：只用 createdAt 夠嗎？兩筆同時間的支出怎麼辦？</li>
     *   <li>JOIN FETCH 集合（shares）時不能直接分頁：先查出本頁的 ID，再依 ID 一次載入明細</li>
     *   <li>如何不額外 COUNT 就知道「還有沒有下一頁」？（提示：多查一筆）</li>
     *   <li>cursor 對前端是不透明字串；被竄改或格式錯誤時要回 400，而不是 500</li>
     * </ol>
     */
    @Transactional(readOnly = true)
    public ExpensePageResponse getGroupExpensePage(Long groupId, int pageSize, String cursor, Long currentUserId) {
        // TODO 練習項目：實作完成後移除下面這行
        throw new BusinessException(ErrorCode.NOT_IMPLEMENTED);
    }
}
