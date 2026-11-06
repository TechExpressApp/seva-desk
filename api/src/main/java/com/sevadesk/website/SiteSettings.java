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
        List<Card> initiatives,
        List<MenuItem> menu,
        Contact contact,
        List<Link> social,
        String calendarUrl,
        Map<String, Page> pages) {

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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Page(String title, String description, String heading, String body) {}

    public SiteSettings {
        initiatives = initiatives == null ? List.of() : initiatives;
        menu = menu == null ? List.of() : menu;
        social = social == null ? List.of() : social;
        pages = pages == null ? Map.of() : pages;
        contact = contact == null ? new Contact(null, null, null, null) : contact;
    }
}
