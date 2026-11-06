package com.sevadesk.donors;

import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.platform.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class DonationService {

    public record NewDonation(UUID contactId, UUID fundId, BigDecimal amount, Donation.PaymentMode paymentMode,
                              String reference, Instant donatedAt, String notes) {}

    private final DonationRepository donations;
    private final FundRepository funds;
    private final ContactRepository contacts;
    private final ReceiptNumbers receiptNumbers;
    private final ApplicationEventPublisher events;

    DonationService(DonationRepository donations, FundRepository funds, ContactRepository contacts,
                    ReceiptNumbers receiptNumbers, ApplicationEventPublisher events) {
        this.donations = donations;
        this.funds = funds;
        this.contacts = contacts;
        this.receiptNumbers = receiptNumbers;
        this.events = events;
    }

    @Transactional
    public Donation record(NewDonation d) {
        var tenant = TenantContext.require();
        Fund fund = funds.findById(d.fundId()).orElseThrow(() -> new IllegalArgumentException("Unknown fund"));
        String donorName = d.contactId() == null ? "Anonymous"
                : contacts.findById(d.contactId()).orElseThrow(() -> new IllegalArgumentException("Unknown contact")).getFullName();
        Instant when = d.donatedAt() == null ? Instant.now() : d.donatedAt();

        var donation = donations.save(new Donation(d.contactId(), fund.getId(), d.amount(), tenant.currency(),
                d.paymentMode(), d.reference(), when, receiptNumbers.next(when), d.notes()));

        events.publishEvent(new DonationRecorded(tenant.id(), donation.getId(), donation.getReceiptNo(), donation.getAmount(),
                fund.getName(), donorName, d.paymentMode().name(), false));
        return donation;
    }

    @Transactional
    public Donation voidDonation(UUID id, String reason) {
        var donation = donations.findById(id).orElseThrow(() -> new IllegalArgumentException("Unknown donation"));
        donation.voidWith(reason);
        events.publishEvent(new DonationRecorded(donation.getTenantId(), donation.getId(), donation.getReceiptNo(),
                donation.getAmount(), null, null, donation.getPaymentMode().name(), true));
        return donation;
    }
}
