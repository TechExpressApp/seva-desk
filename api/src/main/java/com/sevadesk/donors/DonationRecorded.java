package com.sevadesk.donors;

import java.math.BigDecimal;
import java.util.UUID;

/** Published after a donation is saved or voided. Reporting, receipts and thank-you messages listen to it. */
public record DonationRecorded(UUID tenantId, UUID donationId, String receiptNo, BigDecimal amount,
                               String fundName, String donorName, String paymentMode, boolean voided) {}
