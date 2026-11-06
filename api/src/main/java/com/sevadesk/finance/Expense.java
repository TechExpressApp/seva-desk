package com.sevadesk.finance;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Money the temple paid out: to whom (a contact), when, how much and for what. */
@Entity
@Table(name = "expense")
public class Expense extends TenantScopedEntity {

    @Column(name = "contact_id") private UUID contactId;
    @Enumerated(EnumType.STRING)
    private ExpensePurpose purpose;
    @Column(name = "fund_id") private UUID fundId;
    private BigDecimal amount;
    @Column(name = "spent_on") private LocalDate spentOn;
    private String notes;

    protected Expense() {}

    public Expense(UUID contactId, ExpensePurpose purpose, BigDecimal amount, LocalDate spentOn, String notes) {
        update(contactId, purpose, amount, spentOn, notes);
    }

    public void update(UUID contactId, ExpensePurpose purpose, BigDecimal amount, LocalDate spentOn, String notes) {
        this.contactId = contactId;
        this.purpose = purpose;
        this.amount = amount;
        this.spentOn = spentOn;
        this.notes = notes == null || notes.isBlank() ? null : notes.strip();
    }

    public UUID getContactId() { return contactId; }
    public ExpensePurpose getPurpose() { return purpose; }
    public UUID getFundId() { return fundId; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getSpentOn() { return spentOn; }
    public String getNotes() { return notes; }
}
