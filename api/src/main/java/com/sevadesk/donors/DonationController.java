package com.sevadesk.donors;

import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactMatcher;
import com.sevadesk.contacts.ContactRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ProblemDetail;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','FRONT_DESK')")
class DonationController {

    /**
     * Either pick an existing contact (contactId) or give donor details; details are matched to an existing contact
     * by email, phone or full name, and a new contact is created if none match. Neither = anonymous gift.
     */
    record DonationForm(UUID contactId, @NotNull UUID fundId,
                        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
                        @NotNull Donation.PaymentMode paymentMode, @Size(max = 100) String reference,
                        Instant donatedAt, @Size(max = 500) String notes,
                        @Size(max = 100) String donorFirstName, @Size(max = 100) String donorLastName,
                        @Size(max = 200) String donorEmail, @Size(max = 30) String donorPhone) {}

    /** How the donor was linked: EMAIL / PHONE / NAME = existing contact; CREATED = new contact. */
    record DonorLink(UUID contactId, String name, ContactMatcher.MatchedBy matchedBy) {}

    record VoidForm(@NotBlank @Size(max = 300) String reason) {}

    record DonationView(UUID id, String receiptNo, UUID contactId, String donorName, UUID fundId, String fundName,
                        BigDecimal amount, String currency, Donation.PaymentMode paymentMode, String reference,
                        Instant donatedAt, Donation.Status status, String voidReason, String notes, DonorLink donorLink) {}

    record FundView(UUID id, String name, boolean taxDeductible) {}

    record FundForm(@NotBlank @Size(max = 100) String name, boolean taxDeductible) {}

    private final DonationService service;
    private final DonationRepository donations;
    private final FundRepository funds;
    private final ContactRepository contacts;
    private final ContactMatcher matcher;

    DonationController(DonationService service, DonationRepository donations, FundRepository funds,
                       ContactRepository contacts, ContactMatcher matcher) {
        this.service = service;
        this.donations = donations;
        this.funds = funds;
        this.contacts = contacts;
        this.matcher = matcher;
    }

    @GetMapping("/donations")
    @Transactional(readOnly = true)
    Page<DonationView> list(@RequestParam(required = false) UUID contactId,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        var result = contactId == null ? donations.findAllByOrderByDonatedAtDesc(pageable)
                : donations.findByContactIdOrderByDonatedAtDesc(contactId, pageable);
        Map<UUID, String> fundNames = fundNames();
        Map<UUID, String> donorNames = donorNames(result.getContent());
        return result.map(d -> view(d, fundNames, donorNames, null));
    }

    @PostMapping("/donations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER','FRONT_DESK')")
    @Transactional
    DonationView create(@Valid @RequestBody DonationForm f) {
        UUID contactId = f.contactId();
        DonorLink link = null;
        if (contactId == null) {
            var match = matcher.findOrCreate(new ContactMatcher.DonorDetails(f.donorFirstName(), f.donorLastName(),
                    f.donorEmail(), f.donorPhone()), "donation entry");
            if (match.isPresent()) {
                contactId = match.get().contact().getId();
                link = new DonorLink(contactId, match.get().contact().getFullName(), match.get().matchedBy());
            }
        }
        var d = service.record(new DonationService.NewDonation(contactId, f.fundId(), f.amount(), f.paymentMode(),
                f.reference(), f.donatedAt(), f.notes()));
        return view(d, fundNames(), donorNames(List.of(d)), link);
    }

    @PostMapping("/donations/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    DonationView voidDonation(@PathVariable UUID id, @Valid @RequestBody VoidForm f) {
        var d = service.voidDonation(id, f.reason());
        return view(d, fundNames(), donorNames(List.of(d)), null);
    }

    @GetMapping("/funds")
    List<FundView> funds() {
        return funds.findByActiveTrueOrderByName().stream().map(f -> new FundView(f.getId(), f.getName(), f.isTaxDeductible())).toList();
    }

    @PostMapping("/funds")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    FundView createFund(@Valid @RequestBody FundForm f) {
        var fund = funds.save(new Fund(f.name(), f.taxDeductible()));
        return new FundView(fund.getId(), fund.getName(), fund.isTaxDeductible());
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    ProblemDetail badRequest(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private Map<UUID, String> fundNames() {
        return funds.findAll().stream().collect(Collectors.toMap(Fund::getId, Fund::getName, (a, b) -> a));
    }

    private Map<UUID, String> donorNames(List<Donation> list) {
        var ids = list.stream().map(Donation::getContactId).filter(java.util.Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return contacts.findAllById(ids).stream().collect(Collectors.toMap(Contact::getId, Contact::getFullName));
    }

    private static DonationView view(Donation d, Map<UUID, String> fundNames, Map<UUID, String> donorNames, DonorLink link) {
        String donor = d.getContactId() == null ? "Anonymous" : donorNames.getOrDefault(d.getContactId(), "—");
        return new DonationView(d.getId(), d.getReceiptNo(), d.getContactId(), donor, d.getFundId(),
                fundNames.get(d.getFundId()), d.getAmount(), d.getCurrency(), d.getPaymentMode(), d.getReference(),
                d.getDonatedAt(), d.getStatus(), d.getVoidReason(), d.getNotes(), link);
    }
}
