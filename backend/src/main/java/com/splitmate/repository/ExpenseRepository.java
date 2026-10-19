package com.splitmate.repository;

import com.splitmate.dto.MemberAmountTotal;
import com.splitmate.entity.Expense;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /**
     * 一次查出支出、付款人、分攤明細與分攤成員，避免 N+1 查詢。
     * 注意：JOIN FETCH 集合時不能搭配分頁（Hibernate 會在記憶體分頁並發出警告），
     * 若之後單一群組支出量很大，需改成「先分頁查 ID、再依 ID 抓明細」兩段式查詢。
     */
    @Query("""
            SELECT DISTINCT expense
            FROM Expense expense
            JOIN FETCH expense.payer
            LEFT JOIN FETCH expense.shares share
            LEFT JOIN FETCH share.user
            WHERE expense.group.id = :groupId
            ORDER BY expense.createdAt DESC, expense.id DESC
            """)
    List<Expense> findAllWithSharesByGroupId(@Param("groupId") Long groupId);

    /** 每位成員實際付出的總額。在資料庫加總，不必把所有支出載入記憶體 */
    @Query("""
            SELECT new com.splitmate.dto.MemberAmountTotal(expense.payer.id, SUM(expense.amount))
            FROM Expense expense
            WHERE expense.group.id = :groupId
            GROUP BY expense.payer.id
            """)
    List<MemberAmountTotal> sumPaidAmountByGroupId(@Param("groupId") Long groupId);

    /** 每位成員應分攤的總額 */
    @Query("""
            SELECT new com.splitmate.dto.MemberAmountTotal(share.user.id, SUM(share.shareAmount))
            FROM ExpenseShare share
            WHERE share.expense.group.id = :groupId
            GROUP BY share.user.id
            """)
    List<MemberAmountTotal> sumOwedAmountByGroupId(@Param("groupId") Long groupId);
}
