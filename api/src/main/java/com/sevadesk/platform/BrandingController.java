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
                    Map<String, String> terms, List<String> enabledModules, String whatsappNumber) {}

    private final TenantRepository tenants;

    BrandingController(TenantRepository tenants) {
        this.tenants = tenants;
    }

    @GetMapping("/branding")
    ResponseEntity<Branding> branding() {
        Tenant t = tenants.findById(TenantContext.require().id()).orElseThrow();
        var body = new Branding(t.getSlug(), t.getName(), t.getShortName(), t.getGreeting(), t.getTagline(),
                t.getPrimaryColor(), t.getAccentColor(), t.getSoftColor(), t.getLogoUrl(),
                t.getCurrency(), t.getLocale(), t.getTimeZone(),
                t.getTerms(), t.getEnabledModules(), t.getWhatsappNumber());
        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofMinutes(5))).body(body);
    }
}
