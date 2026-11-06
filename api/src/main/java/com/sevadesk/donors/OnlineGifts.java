package com.sevadesk.donors;

import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
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
                           Instant paidAt, String note) {}

    private final DonationRepository donations;
    private final FundRepository funds;
    private final ContactRepository contacts;
    private final DonationService donationService;

    OnlineGifts(DonationRepository donations, FundRepository funds, ContactRepository contacts,
                DonationService donationService) {
        this.donations = donations;
        this.funds = funds;
        this.contacts = contacts;
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

        var contactId = gift.email() == null || gift.email().isBlank() ? null
                : contacts.findFirstByEmailIgnoreCase(gift.email().trim())
                    .orElseGet(() -> contacts.save(newContact(gift)))
                    .getId();

        String note = gift.note() == null ? "Online (Stripe)" : ("Online (Stripe) · " + gift.note());
        if (note.length() > 500) note = note.substring(0, 500);

        var donation = donationService.record(new DonationService.NewDonation(contactId, fund.getId(), gift.amount(),
                Donation.PaymentMode.ONLINE, gift.reference(), gift.paidAt(), note));
        log.info("Recorded online gift {} as receipt {}", gift.reference(), donation.getReceiptNo());
        return Optional.of(donation);
    }

    private static Contact newContact(PaidGift gift) {
        String name = gift.donorName() == null || gift.donorName().isBlank() ? gift.email() : gift.donorName().trim();
        int space = name.indexOf(' ');
        var c = new Contact(space > 0 ? name.substring(0, space) : name, space > 0 ? name.substring(space + 1) : null);
        c.setEmail(gift.email().trim().toLowerCase(Locale.ROOT));
        c.setCustomFields(Map.of("source", "online donation"));
        return c;
    }
}
