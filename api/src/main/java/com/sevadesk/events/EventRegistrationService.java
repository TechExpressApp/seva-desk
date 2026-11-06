package com.sevadesk.events;

import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactMatcher;
import com.sevadesk.contacts.ContactMatcher.DonorDetails;
import com.sevadesk.platform.Mailer;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Records event registrations. Each registrant is matched to an existing contact (email; WhatsApp number when the
 * first name matches too, since families share numbers; then full name) or saved as a new one, and the temple's
 * notification address is emailed about the registration.
 */
@Service
public class EventRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(EventRegistrationService.class);

    public record NewRegistration(String eventName, String firstName, String lastName, String email,
                                  String whatsapp, String city, String state, int age) {}

    public record Registered(EventRegistration registration, ContactMatcher.MatchedBy contactMatchedBy) {}

    private final EventRegistrationRepository registrations;
    private final EventRepository events;
    private final ContactMatcher contacts;
    private final TenantRepository tenants;
    private final Mailer mailer;

    EventRegistrationService(EventRegistrationRepository registrations, EventRepository events,
                             ContactMatcher contacts, TenantRepository tenants, Mailer mailer) {
        this.registrations = registrations;
        this.events = events;
        this.contacts = contacts;
        this.tenants = tenants;
        this.mailer = mailer;
    }

    /** Saves the registration and the contact together; nothing is saved if either fails. */
    @Transactional
    public Registered register(NewRegistration r) {
        String eventName = r.eventName().strip();
        var event = events.findFirstByTitleIgnoreCaseOrderByStartsAtDesc(eventName);

        var match = contacts.findOrCreate(
                new DonorDetails(r.firstName(), r.lastName(), r.email(), r.whatsapp()),
                "Event registration: " + eventName, true).orElseThrow();   // shared family numbers stay separate
        Contact c = match.contact();
        if (c.getCity() == null || c.getCity().isBlank()) c.setCity(r.city().strip());
        Map<String, Object> fields = new HashMap<>(c.getCustomFields());
        fields.putIfAbsent("state", r.state().strip());
        fields.put("age", r.age());                               // latest age given
        c.setCustomFields(fields);

        var saved = registrations.save(new EventRegistration(event.map(Event::getId).orElse(null),
                event.map(Event::getTitle).orElse(eventName), c.getId(), r.firstName().strip(), r.lastName().strip(),
                r.email().strip().toLowerCase(Locale.ROOT), r.whatsapp().strip(), r.city().strip(), r.state().strip(),
                r.age()));
        log.info("Registration {} for '{}' (contact {}, {})", saved.getId(), saved.getEventName(), c.getId(),
                match.matchedBy());
        return new Registered(saved, match.matchedBy());
    }

    /**
     * Emails the registration to the temple's notification address. Called after the registration is saved, so a
     * mail problem never loses a registration. @return true if the email was handed to the mail server
     */
    public boolean notifyTemple(Registered result) {
        var tenant = tenants.findById(TenantContext.require().id()).orElseThrow();
        String to = tenant.getNotificationEmail();
        if (to == null || to.isBlank()) return false;
        var r = result.registration();
        String when = DateTimeFormatter.ofPattern("EEE d MMM yyyy, h:mm a", Locale.US)
                .format(r.getCreatedAt().atZone(zone(tenant.getTimeZone())));
        String contactNote = switch (result.contactMatchedBy()) {
            case CREATED -> "New contact added to SevaDesk.";
            case EMAIL -> "Matched an existing contact by email.";
            case PHONE -> "Matched an existing contact by WhatsApp number.";
            case NAME -> "Matched an existing contact by name.";
        };
        String text = """
                Hare Krishna!

                A new registration was received for %s.

                Name:      %s %s
                Email:     %s
                WhatsApp:  %s
                City:      %s
                State:     %s
                Age:       %d

                Registered: %s
                %s

                -- SevaDesk, %s
                """.formatted(r.getEventName(), r.getFirstName(), r.getLastName(), r.getEmail(), r.getWhatsapp(),
                r.getCity(), r.getState(), r.getAge(), when, contactNote, tenant.getName());
        return mailer.send(to, "New registration: " + r.getEventName() + " – " + r.getFirstName() + " " + r.getLastName(), text);
    }

    private static ZoneId zone(String id) {
        try {
            return ZoneId.of(id);
        } catch (Exception e) {
            return ZoneId.of("UTC");
        }
    }
}
