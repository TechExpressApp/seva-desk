package com.sevadesk.donors;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A gift. Never deleted: mistakes are voided with a reason so receipt numbers stay gap-free and auditable. */
@Entity
@Table(name = "donation")
public class Donation extends TenantScopedEntity {

    /** UPI is offered to rupee (India) temples, Zelle to US-dollar temples; see BrandingController.paymentModes. */
    public enum PaymentMode { CASH, CHEQUE, UPI, ZELLE, CARD, BANK_TRANSFER, ONLINE, IN_KIND }
    public enum Status { RECORDED, VOIDED }

    @Column(name = "contact_id") private UUID contactId;
    @Column(name = "fund_id") private UUID fundId;
    private BigDecimal amount;
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_mode") private PaymentMode paymentMode;
    private String reference;
    @Column(name = "donated_at") private Instant donatedAt;
    @Column(name = "receipt_no") private String receiptNo;
    @Enumerated(EnumType.STRING)
    private Status status = Status.RECORDED;
    @Column(name = "void_reason") private String voidReason;
    private String notes;

    protected Donation() {}

    Donation(UUID contactId, UUID fundId, BigDecimal amount, String currency, PaymentMode paymentMode,
             String reference, Instant donatedAt, String receiptNo, String notes) {
        this.contactId = contactId;
        this.fundId = fundId;
        this.amount = amount;
        this.currency = currency;
        this.paymentMode = paymentMode;
        this.reference = reference;
        this.donatedAt = donatedAt;
        this.receiptNo = receiptNo;
        this.notes = notes;
    }

    void voidWith(String reason) {
        if (status == Status.VOIDED) throw new IllegalStateException("Donation is already voided");
        status = Status.VOIDED;
        voidReason = reason;
    }

    public UUID getContactId() { return contactId; }
    public UUID getFundId() { return fundId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMode getPaymentMode() { return paymentMode; }
    public String getReference() { return reference; }
    public Instant getDonatedAt() { return donatedAt; }
    public String getReceiptNo() { return receiptNo; }
    public Status getStatus() { return status; }
    public String getVoidReason() { return voidReason; }
    public String getNotes() { return notes; }
}
