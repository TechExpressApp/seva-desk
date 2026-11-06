package com.sevadesk.website;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.contacts.Family;
import com.sevadesk.events.EventService;
import com.sevadesk.platform.Tenant;
import com.sevadesk.platform.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The temple's public website, rendered on the server so search engines see complete pages.
 * Events come straight from SevaDesk: an event saved as PUBLISHED in the admin appears on the next page load.
 * Page URLs match the temple's old site (e.g. /bhakti-vriksha, /donate) so existing search rankings carry over.
 */
@Controller
class WebsiteController {

    private static final Logger log = LoggerFactory.getLogger(WebsiteController.class);

    private final EventService events;
    private final ContactRepository contacts;
    private final ObjectMapper json;

    WebsiteController(EventService events, ContactRepository contacts, ObjectMapper json) {
        this.events = events;
        this.contacts = contacts;
        this.json = json;
    }

    // ---- pages ----------------------------------------------------------------------------------------------

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("events", events.upcomingPublished(6));
        var temple = (Tenant) model.getAttribute("temple");
        var contact = site(model).contact();
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "HinduTemple");
        ld.put("name", temple.getName());
        ld.put("url", model.getAttribute("baseUrl"));
        if (contact.phone() != null) ld.put("telephone", contact.phone());
        if (contact.email() != null) ld.put("email", contact.email());
        if (contact.address() != null) ld.put("address", contact.address());
        model.addAttribute("jsonLd", jsonLd(ld));
        return "site/home";
    }

    @GetMapping("/events")
    String events(Model model) {
        model.addAttribute("events", events.upcomingPublished(100));
        return "site/events";
    }

    @GetMapping("/events/{slug}")
    String event(@PathVariable String slug, Model model, HttpServletResponse response) {
        var event = events.publishedBySlug(slug);
        if (event.isEmpty()) return notFound(response);
        var e = event.get();
        model.addAttribute("event", e);
        // schema.org Event: lets Google show the event with its date in search results.
        var fmt = (WebsiteModel.EventFormat) model.getAttribute("fmt");
        Map<String, Object> ld = new LinkedHashMap<>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "Event");
        ld.put("name", e.getTitle());
        ld.put("startDate", fmt.iso(e.getStartsAt()));
        if (e.getEndsAt() != null) ld.put("endDate", fmt.iso(e.getEndsAt()));
        ld.put("eventAttendanceMode", "https://schema.org/OfflineEventAttendanceMode");
        ld.put("eventStatus", "https://schema.org/EventScheduled");
        if (e.getSummary() != null) ld.put("description", e.getSummary());
        if (e.getImageUrl() != null) ld.put("image", e.getImageUrl());
        var temple = (Tenant) model.getAttribute("temple");
        ld.put("location", Map.of("@type", "Place", "name", e.getLocation() != null ? e.getLocation() : temple.getName(),
                "address", site(model).contact().address() != null ? site(model).contact().address() : temple.getName()));
        ld.put("organizer", Map.of("@type", "Organization", "name", temple.getName(), "url", model.getAttribute("baseUrl")));
        model.addAttribute("jsonLd", jsonLd(ld));
        return "site/event";
    }

    @GetMapping("/contact-us")
    String contact(Model model, @RequestParam(required = false) String subscribed,
                   @RequestParam(required = false) String invalid) {
        model.addAttribute("page", site(model).pages().get("contact-us"));
        model.addAttribute("invalid", invalid != null);
        model.addAttribute("subscribed", subscribed != null);
        return "site/contact";
    }

    /** "Subscribe for event notifications" on the contact page: becomes a contact in SevaDesk with email consent. */
    @PostMapping("/subscribe")
    @Transactional
    String subscribe(@RequestParam String firstName, @RequestParam(required = false) String lastName,
                     @RequestParam String email, @RequestParam(required = false) String whatsapp,
                     @RequestParam(required = false) String initiatedName,
                     @RequestParam(required = false) String spiritualMaster,
                     @RequestParam(required = false) LocalDate birthDate,
                     @RequestParam(required = false) String spouseName,
                     @RequestParam(required = false) LocalDate spouseBirthDate,
                     @RequestParam(required = false) String child1Name,
                     @RequestParam(required = false) LocalDate child1BirthDate,
                     @RequestParam(required = false) String child2Name,
                     @RequestParam(required = false) LocalDate child2BirthDate,
                     @RequestParam(required = false) String child3Name,
                     @RequestParam(required = false) LocalDate child3BirthDate,
                     @RequestParam(required = false, name = "website") String honeypot) {
        if (honeypot != null && !honeypot.isBlank()) return "redirect:/contact-us?subscribed"; // bots
        if (firstName.isBlank() || !email.contains("@") || firstName.length() > 100 || email.length() > 200) {
            return "redirect:/contact-us?invalid";
        }
        var c = new Contact(firstName.trim(), blankToNull(lastName));
        c.setEmail(email.trim().toLowerCase(Locale.ROOT));
        c.setPhone(blankToNull(whatsapp));
        c.setBirthDate(birthDate);
        c.setConsentEmail(true);
        c.setConsentWhatsapp(whatsapp != null && !whatsapp.isBlank());
        Map<String, Object> extra = new LinkedHashMap<>();
        if (initiatedName != null && !initiatedName.isBlank()) extra.put("initiatedName", initiatedName.trim());
        if (spiritualMaster != null && !spiritualMaster.isBlank()) extra.put("spiritualMaster", spiritualMaster.trim());
        extra.put("source", "website subscribe form");
        c.setCustomFields(extra);
        c.setFamily(new Family(member(spouseName, spouseBirthDate), java.util.List.of(
                member(child1Name, child1BirthDate), member(child2Name, child2BirthDate), member(child3Name, child3BirthDate))));
        contacts.save(c);
        log.info("New website subscriber for {}", TenantContext.require().slug());
        return "redirect:/contact-us?subscribed";
    }

    /** Old site URLs such as /bhakti-vriksha, /donate, /guru-vandana, /category/all-products. */
    @GetMapping({"/{slug:[a-z0-9][a-z0-9-]*}", "/category/{slug:[a-z0-9][a-z0-9-]*}"})
    String page(@PathVariable String slug, Model model, HttpServletRequest request, HttpServletResponse response) {
        String key = request.getRequestURI().startsWith("/category/") ? "category/" + slug : slug;
        var page = site(model).pages().get(key);
        if (page == null) return notFound(response);
        model.addAttribute("page", page);
        model.addAttribute("pageKey", key);
        return "site/page";
    }

    private static String notFound(HttpServletResponse response) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        return "site/not-found";
    }

    private static Family.Member member(String name, LocalDate birthDate) {
        String n = blankToNull(name);
        if (n != null && n.length() > 100) n = n.substring(0, 100);
        return new Family.Member(n, birthDate);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** JSON for a <script type="application/ld+json"> block; "</" is escaped so text can't close the script tag. */
    private String jsonLd(Map<String, Object> data) {
        try {
            return json.writeValueAsString(data).replace("</", "<\\/");
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return "{}";
        }
    }

    private static SiteSettings site(Model model) {
        return (SiteSettings) model.getAttribute("site");
    }
}
