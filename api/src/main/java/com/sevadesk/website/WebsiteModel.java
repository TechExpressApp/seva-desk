package com.sevadesk.website;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.events.Event;
import com.sevadesk.platform.Tenant;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/** Data every public page needs (temple, website settings, colours, dates), shared by the website controllers. */
@ControllerAdvice(assignableTypes = {WebsiteController.class, DonateController.class, RegisterController.class})
class WebsiteModel {

    private final TenantRepository tenants;
    private final ObjectMapper json;

    WebsiteModel(TenantRepository tenants, ObjectMapper json) {
        this.tenants = tenants;
        this.json = json;
    }

    @ModelAttribute("temple")
    Tenant temple() {
        return tenants.findById(TenantContext.require().id()).orElseThrow();
    }

    @ModelAttribute("site")
    SiteSettings site(@ModelAttribute("temple") Tenant temple) {
        return json.convertValue(temple.getSite() == null ? Map.of() : temple.getSite(), SiteSettings.class);
    }

    /** Brand colours for the page CSS; anything that isn't a plain hex colour falls back to the default. */
    @ModelAttribute("colors")
    Colors colors(@ModelAttribute("temple") Tenant temple) {
        return new Colors(hex(temple.getPrimaryColor(), "#B5462B"), hex(temple.getAccentColor(), "#E8890C"),
                hex(temple.getSoftColor(), "#FFF4E3"));
    }

    @ModelAttribute("fmt")
    EventFormat fmt() {
        return new EventFormat(TenantContext.require().zone());
    }

    @ModelAttribute
    void common(Model model, HttpServletRequest request) {
        model.addAttribute("year", Year.now().getValue());
        model.addAttribute("path", request.getRequestURI());
        model.addAttribute("baseUrl", ServletUriComponentsBuilder.fromCurrentContextPath().toUriString());
    }

    record Colors(String brand, String accent, String soft) {}

    private static String hex(String value, String fallback) {
        return value != null && value.matches("#[0-9A-Fa-f]{3,8}") ? value : fallback;
    }

    /** Date formatting in the temple's own time zone, for templates. */
    record EventFormat(ZoneId zone) {
        private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.ENGLISH);
        private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
        private static final DateTimeFormatter LONG = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
        private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

        public String day(Instant i) { return DAY.format(i.atZone(zone)); }
        public String month(Instant i) { return MONTH.format(i.atZone(zone)); }
        public String date(Instant i) { return LONG.format(i.atZone(zone)); }
        public String time(Instant i) { return TIME.format(i.atZone(zone)); }
        public String iso(Instant i) { return i.atZone(zone).toOffsetDateTime().toString(); }

        public String when(Event e) {
            String s = date(e.getStartsAt()) + " · " + time(e.getStartsAt());
            if (e.getEndsAt() == null) return s;
            boolean sameDay = e.getEndsAt().atZone(zone).toLocalDate().equals(e.getStartsAt().atZone(zone).toLocalDate());
            return s + " – " + (sameDay ? time(e.getEndsAt()) : date(e.getEndsAt()) + " · " + time(e.getEndsAt()));
        }
    }
}
