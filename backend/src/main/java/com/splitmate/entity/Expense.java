package com.splitmate.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false, updatable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_user_id", nullable = false)
    private User payer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String description;

    @Column(name = "receipt_path")
    private String receiptPath;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // cascade + orphanRemoval：分攤明細的生命週期完全跟著支出，儲存支出時一併儲存
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExpenseShare> shares = new ArrayList<>();

    /** JPA 規範要求的無參數建構子，不供業務程式使用 */
    protected Expense() {
    }

    public Expense(ExpenseGroup group, User payer, BigDecimal amount, String description, User createdBy) {
        this.group = group;
        this.payer = payer;
        this.amount = amount;
        this.description = description;
        this.createdBy = createdBy;
    }

    /** 透過此方法新增分攤，確保雙向關聯兩端一致 */
    public void addShare(User user, BigDecimal shareAmount) {
        shares.add(new ExpenseShare(this, user, shareAmount));
    }

    public void attachReceipt(String receiptPath) {
        this.receiptPath = receiptPath;
    }

    public boolean hasReceipt() {
        return receiptPath != null;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public User getPayer() {
        return payer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public String getReceiptPath() {
        return receiptPath;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** 回傳唯讀清單，外部只能透過 addShare 修改 */
    public List<ExpenseShare> getShares() {
        return Collections.unmodifiableList(shares);
    }
}
