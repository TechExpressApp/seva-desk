package com.sevadesk.website;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.events.EventService;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

/** robots.txt and sitemap.xml for search engines. The admin (/admin) and API (/api) are kept out of search results. */
@RestController
class SeoController {

    private final TenantRepository tenants;
    private final EventService events;
    private final ObjectMapper json;

    SeoController(TenantRepository tenants, EventService events, ObjectMapper json) {
        this.tenants = tenants;
        this.events = events;
        this.json = json;
    }

    @GetMapping(path = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    String robots() {
        return """
                User-agent: *
                Disallow: /admin/
                Disallow: /api/
                Sitemap: %s/sitemap.xml
                """.formatted(base());
    }

    @GetMapping(path = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    String sitemap() {
        var tenant = tenants.findById(TenantContext.require().id()).orElseThrow();
        var site = json.convertValue(tenant.getSite() == null ? Map.of() : tenant.getSite(), SiteSettings.class);
        String base = base();
        var xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
                """);
        url(xml, base + "/");
        url(xml, base + "/events");
        url(xml, base + "/contact-us");
        site.pages().keySet().stream().filter(k -> !k.equals("contact-us")).sorted().forEach(k -> url(xml, base + "/" + k));
        events.upcomingPublished(500).forEach(e -> url(xml, base + "/events/" + e.getSlug()));
        return xml.append("</urlset>\n").toString();
    }

    private static void url(StringBuilder xml, String loc) {
        xml.append("  <url><loc>").append(loc.replace("&", "&amp;")).append("</loc></url>\n");
    }

    private static String base() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    }
}
