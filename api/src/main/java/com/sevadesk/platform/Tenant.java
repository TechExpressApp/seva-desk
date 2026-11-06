package com.sevadesk.platform;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A temple (or any place of worship) using the platform: its identity, branding and settings. Read-only for the app role. */
@Entity
@Table(name = "tenant")
public class Tenant {

    @Id
    private UUID id;
    private String slug;
    @Column(name = "custom_domain")
    private String customDomain;
    private String name;
    @Column(name = "short_name")
    private String shortName;
    private String greeting;
    private String tagline;
    @Column(name = "primary_color")
    private String primaryColor;
    @Column(name = "accent_color")
    private String accentColor;
    @Column(name = "soft_color")
    private String softColor;
    @Column(name = "logo_url")
    private String logoUrl;
    private String currency;
    private String locale;
    @Column(name = "time_zone")
    private String timeZone;
    @Column(name = "fiscal_year_start")
    private int fiscalYearStart;
    @Column(name = "tax_registration")
    private String taxRegistration;
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> terms;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_modules")
    private List<String> enabledModules;
    @Column(name = "whatsapp_number")
    private String whatsappNumber;
    private boolean active;
    /** Public website settings (menu, contact, pages). Read by the website module as SiteSettings. */
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> site;

    protected Tenant() {}

    public TenantContext.TenantRef toRef() {
        return new TenantContext.TenantRef(id, slug, shortName, currency, ZoneId.of(timeZone), fiscalYearStart);
    }

    public UUID getId() { return id; }
    public String getSlug() { return slug; }
    public String getCustomDomain() { return customDomain; }
    public String getName() { return name; }
    public String getShortName() { return shortName; }
    public String getGreeting() { return greeting; }
    public String getTagline() { return tagline; }
    public String getPrimaryColor() { return primaryColor; }
    public String getAccentColor() { return accentColor; }
    public String getSoftColor() { return softColor; }
    public String getLogoUrl() { return logoUrl; }
    public String getCurrency() { return currency; }
    public String getLocale() { return locale; }
    public String getTimeZone() { return timeZone; }
    public int getFiscalYearStart() { return fiscalYearStart; }
    public String getTaxRegistration() { return taxRegistration; }
    public Map<String, String> getTerms() { return terms; }
    public List<String> getEnabledModules() { return enabledModules; }
    public String getWhatsappNumber() { return whatsappNumber; }
    public boolean isActive() { return active; }
    public Map<String, Object> getSite() { return site; }
}
