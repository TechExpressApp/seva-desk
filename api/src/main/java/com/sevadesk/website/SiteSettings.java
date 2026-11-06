package com.sevadesk.website;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * A temple's public website, stored as JSON in tenant.site. Everything a temple would want to change
 * without a developer lives here: menu, contact details, home page text and the content of each page.
 * Page bodies are HTML written by the temple's own staff.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SiteSettings(
        String heroTitle,
        String heroText,
        String mantra,
        String quote,
        String quoteSource,
        String welcomeTitle,
        String welcomeText,
        Gatherings gatherings,
        List<Card> initiatives,
        List<MenuItem> menu,
        Contact contact,
        List<Link> social,
        String calendarUrl,
        Map<String, Page> pages,
        Donate donate) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MenuItem(String label, String href, List<MenuItem> children) {
        public boolean hasChildren() { return children != null && !children.isEmpty(); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(String phone, String email, String address, String mapUrl) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Link(String label, String url) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(String title, String text, String href) {}

    /** "Come together for Weekly Gatherings" at the top of the home page. image is optional (a URL). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Gatherings(String eyebrow, String title, List<Gathering> items) {
        public Gatherings {
            items = items == null ? List.of() : items;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Gathering(String title, String when, String text, String image, String href, String button) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(String title, String description, String heading, String body) {}

    /** The /donate page: ways to give, card-giving options (paid through Stripe) and campaigns. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Donate(String heading, String intro, String quote, List<Offline> offline, List<String> frequencies,
                         List<Option> options, String defaultFund, boolean allowOther, boolean coverFees,
                         List<Campaign> campaigns) {
        public Donate {
            offline = offline == null ? List.of() : offline;
            frequencies = frequencies == null || frequencies.isEmpty() ? List.of("ONE_TIME") : frequencies;
            options = options == null ? List.of() : options;
            campaigns = campaigns == null ? List.of() : campaigns;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Offline(String label, String detail) {}

    /** A suggested amount, e.g. "Sunday Feast Prasadam Sponsor", $151, fund "Sunday Feast". */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Option(String label, java.math.BigDecimal amount, String fund) {}

    /**
     * A named appeal with its own button, e.g. 1000 Fortunate Souls: $200 monthly for 25 months.
     * amount null = the donor chooses; installments null = until the donor cancels.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Campaign(String key, String title, String text, String fund, java.math.BigDecimal amount,
                           String frequency, Integer installments, String button) {}

    public SiteSettings {
        initiatives = initiatives == null ? List.of() : initiatives;
        menu = menu == null ? List.of() : menu;
        social = social == null ? List.of() : social;
        pages = pages == null ? Map.of() : pages;
        contact = contact == null ? new Contact(null, null, null, null) : contact;
    }
}
