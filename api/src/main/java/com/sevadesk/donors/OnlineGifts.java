package com.sevadesk.donors;

import com.sevadesk.contacts.ContactMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

/**
 * Turns a paid online gift (from the payment provider's webhook) into a SevaDesk donation with a receipt number.
 * Safe to call more than once for the same payment: the reference is checked first and is unique in the database.
 */
@Service
public class OnlineGifts {

    private static final Logger log = LoggerFactory.getLogger(OnlineGifts.class);

    /** A completed payment. {@code reference} is the provider's payment or invoice id. */
    public record PaidGift(String reference, BigDecimal amount, String fundName, String email, String donorName,
                           String phone, Instant paidAt, String note) {}

    private final DonationRepository donations;
    private final FundRepository funds;
    private final ContactMatcher matcher;
    private final DonationService donationService;

    OnlineGifts(DonationRepository donations, FundRepository funds, ContactMatcher matcher,
                DonationService donationService) {
        this.donations = donations;
        this.funds = funds;
        this.matcher = matcher;
        this.donationService = donationService;
    }

    /** @return the new donation, or empty if this payment was already recorded */
    @Transactional
    public Optional<Donation> record(PaidGift gift) {
        if (donations.existsByPaymentModeAndReference(Donation.PaymentMode.ONLINE, gift.reference())) {
            log.info("Online gift {} already recorded; ignoring repeat", gift.reference());
            return Optional.empty();
        }
        String fundName = gift.fundName() == null || gift.fundName().isBlank() ? "General" : gift.fundName().trim();
        Fund fund = funds.findByName(fundName).orElseGet(() -> funds.save(new Fund(fundName, true)));

        // Same person by email, phone or name -> same contact, so their giving adds up; otherwise a new contact.
        var contactId = matcher.findOrCreate(
                        ContactMatcher.DonorDetails.fromFullName(gift.donorName(), gift.email(), gift.phone()),
                        "online donation")
                .map(m -> m.contact().getId()).orElse(null);

        String note = gift.note() == null ? "Online (Stripe)" : ("Online (Stripe) · " + gift.note());
        if (note.length() > 500) note = note.substring(0, 500);

        var donation = donationService.record(new DonationService.NewDonation(contactId, fund.getId(), gift.amount(),
                Donation.PaymentMode.ONLINE, gift.reference(), gift.paidAt(), note));
        log.info("Recorded online gift {} as receipt {}", gift.reference(), donation.getReceiptNo());
        return Optional.of(donation);
    }
}
