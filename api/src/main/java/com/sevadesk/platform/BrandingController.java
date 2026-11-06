package com.sevadesk.platform;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Public: the web app calls this before its first paint to theme itself for the temple at this address. */
@RestController
@RequestMapping("/api/public")
class BrandingController {

    record Branding(String slug, String name, String shortName, String greeting, String tagline,
                    String primaryColor, String accentColor, String softColor, String logoUrl,
                    String currency, String locale, String timeZone,
                    Map<String, String> terms, List<String> enabledModules, String whatsappNumber,
                    List<PaymentModeOption> paymentModes) {}

    /** A way donations are received, as offered on the donation form. */
    record PaymentModeOption(String value, String label) {}

    private final TenantRepository tenants;

    /**
     * Payment modes for the donation form, by country: UPI for rupee temples, Zelle and "Check" for US-dollar temples.
     * The values match com.sevadesk.donors.Donation.PaymentMode.
     */
    static List<PaymentModeOption> paymentModes(String currency) {
        boolean india = "INR".equalsIgnoreCase(currency);
        boolean usa = "USD".equalsIgnoreCase(currency);
        var modes = new java.util.ArrayList<PaymentModeOption>();
        modes.add(new PaymentModeOption("CASH", "Cash"));
        if (india) modes.add(new PaymentModeOption("UPI", "UPI"));
        if (usa) modes.add(new PaymentModeOption("ZELLE", "Zelle"));
        modes.add(new PaymentModeOption("CARD", "Card"));
        modes.add(new PaymentModeOption("CHEQUE", usa ? "Check" : "Cheque"));
        modes.add(new PaymentModeOption("BANK_TRANSFER", "Bank transfer"));
        modes.add(new PaymentModeOption("ONLINE", "Online"));
        modes.add(new PaymentModeOption("IN_KIND", "In kind"));
        return modes;
    }

    BrandingController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/branding")
    ResponseEntity<Branding> branding() {
        Tenant t = tenants.findById(TenantContext.require().id()).orElseThrow();
        var body = new Branding(t.getSlug(), t.getName(), t.getShortName(), t.getGreeting(), t.getTagline(),
                t.getPrimaryColor(), t.getAccentColor(), t.getSoftColor(), t.getLogoUrl(),
                t.getCurrency(), t.getLocale(), t.getTimeZone(),
                t.getTerms(), t.getEnabledModules(), t.getWhatsappNumber(), paymentModes(t.getCurrency()));
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5))).body(body);
    }
}
