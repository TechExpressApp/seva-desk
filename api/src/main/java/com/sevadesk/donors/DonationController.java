package com.sevadesk.donors;

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
class DonationController {

    record DonationForm(UUID contactId, @NotNull UUID fundId,
                        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
                        @NotNull Donation.PaymentMode paymentMode, @Size(max = 100) String reference,
                        Instant donatedAt, @Size(max = 500) String notes) {}

    record VoidForm(@NotBlank @Size(max = 300) String reason) {}

    record DonationView(UUID id, String receiptNo, UUID contactId, UUID fundId, String fundName, BigDecimal amount,
                        String currency, Donation.PaymentMode paymentMode, String reference, Instant donatedAt,
                        Donation.Status status, String voidReason, String notes) {}

    record FundView(UUID id, String name, boolean taxDeductible) {}

    record FundForm(@NotBlank @Size(max = 100) String name, boolean taxDeductible) {}

    private final DonationService service;
    private final DonationRepository donations;
    private final FundRepository funds;

    DonationController(DonationService service, DonationRepository donations, FundRepository funds) {
        this.service = service;
        this.donations = donations;
        this.funds = funds;
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
        return result.map(d -> view(d, fundNames));
    }

    @PostMapping("/donations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER','FRONT_DESK')")
    DonationView create(@Valid @RequestBody DonationForm f) {
        var d = service.record(new DonationService.NewDonation(f.contactId(), f.fundId(), f.amount(), f.paymentMode(),
                f.reference(), f.donatedAt(), f.notes()));
        return view(d, fundNames());
    }

    @PostMapping("/donations/{id}/void")
    @PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
    DonationView voidDonation(@PathVariable UUID id, @Valid @RequestBody VoidForm f) {
        return view(service.voidDonation(id, f.reason()), fundNames());
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

    private static DonationView view(Donation d, Map<UUID, String> fundNames) {
        return new DonationView(d.getId(), d.getReceiptNo(), d.getContactId(), d.getFundId(), fundNames.get(d.getFundId()),
                d.getAmount(), d.getCurrency(), d.getPaymentMode(), d.getReference(), d.getDonatedAt(),
                d.getStatus(), d.getVoidReason(), d.getNotes());
    }
}
