package com.sevadesk.finance;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "expense")
public class Expense extends TenantScopedEntity {

    private String category;
    @Column(name = "fund_id") private UUID fundId;
    private BigDecimal amount;
    @Column(name = "spent_on") private LocalDate spentOn;
    private String payee;
    private String notes;

    protected Expense() {}

    public Expense(String category, UUID fundId, BigDecimal amount, LocalDate spentOn, String payee, String notes) {
        this.category = category;
        this.fundId = fundId;
        this.amount = amount;
        this.spentOn = spentOn;
        this.payee = payee;
        this.notes = notes;
    }

    public String getCategory() { return category; }
    public UUID getFundId() { return fundId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getSpentOn() { return spentOn; }
    public String getPayee() { return payee; }
    public String getNotes() { return notes; }
}
