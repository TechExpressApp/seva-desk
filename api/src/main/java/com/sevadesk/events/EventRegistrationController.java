package com.sevadesk.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin API for event registrations. Registering also saves the person as a contact (or matches an existing one)
 * and emails the temple's notification address.
 */
@RestController
@RequestMapping("/api/event-registrations")
@PreAuthorize("hasAnyRole('ADMIN','EVENT_COORDINATOR','FRONT_DESK')")
class EventRegistrationController {

    record RegistrationForm(
            @NotBlank @Size(max = 200) String eventName,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 40) @Pattern(regexp = "^\\+?[0-9 ()\\-.]{7,40}$", message = "must be a phone number")
            String whatsapp,
            @NotBlank @Size(max = 100) String city,
            @NotBlank @Size(max = 100) String state,
            @NotNull @Min(0) @Max(120) Integer age) {}

    /**
     * registrationDate: the day the registration was created, in the temple's time zone (yyyy-MM-dd);
     * registeredAt: the exact moment.
     */
    record RegistrationView(UUID id, UUID eventId, String eventName, UUID contactId, String firstName,
                            String lastName, String email, String whatsapp, String city, String state, int age,
                            java.time.LocalDate registrationDate, Instant registeredAt) {
        static RegistrationView of(EventRegistration r) {
            return new RegistrationView(r.getId(), r.getEventId(), r.getEventName(), r.getContactId(),
                    r.getFirstName(), r.getLastName(), r.getEmail(), r.getWhatsapp(), r.getCity(), r.getState(),
                    r.getAge(), r.getCreatedAt().atZone(com.sevadesk.platform.TenantContext.require().zone()).toLocalDate(),
                    r.getCreatedAt());
        }
    }

    /** A choice for the registration form's Event field: an upcoming event (with its date) or a name used before. */
    record EventOption(String name, Instant startsAt) {}

    /** What happened: the saved registration, how the contact was found, and whether the email went out. */
    record RegistrationResult(RegistrationView registration, String contact, boolean emailSent) {}

    private final EventRegistrationService service;
    private final EventRegistrationRepository registrations;
    private final EventRepository events;

    EventRegistrationController(EventRegistrationService service, EventRegistrationRepository registrations,
                                EventRepository events) {
        this.service = service;
        this.registrations = registrations;
        this.events = events;
    }

    /** Upcoming published events first (soonest first), then other names already used for registrations. */
    @GetMapping("/event-options")
    @Transactional(readOnly = true)
    java.util.List<EventOption> eventOptions() {
        var options = new java.util.ArrayList<EventOption>();
        var seen = new java.util.HashSet<String>();
        for (var e : events.upcoming(Instant.now(), PageRequest.of(0, 50))) {
            if (seen.add(e.getTitle().toLowerCase(java.util.Locale.ROOT))) options.add(new EventOption(e.getTitle(), e.getStartsAt()));
        }
        for (var name : registrations.recentEventNames()) {
            if (seen.add(name.toLowerCase(java.util.Locale.ROOT))) options.add(new EventOption(name, null));
        }
        return options;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    RegistrationResult register(@Valid @RequestBody RegistrationForm f) {
        var result = service.register(new EventRegistrationService.NewRegistration(f.eventName(), f.firstName(),
                f.lastName(), f.email(), f.whatsapp(), f.city(), f.state(), f.age()));
        boolean emailed = service.notifyTemple(result);              // after the save has been committed
        return new RegistrationResult(RegistrationView.of(result.registration()),
                result.contactMatchedBy().name(), emailed);
    }

    /** Newest first. Filter by event name (exact, ignoring case) and/or contact. */
    @GetMapping
    @Transactional(readOnly = true)
    Page<RegistrationView> list(@RequestParam(required = false) String eventName,
                                @RequestParam(required = false) UUID contactId,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "25") int size) {
        String name = eventName == null || eventName.isBlank() ? null : eventName.strip();
        return registrations.search(name, contactId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)))
                .map(RegistrationView::of);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    RegistrationView get(@PathVariable UUID id) {
        return RegistrationView.of(registrations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)));
    }

    /** Removes a registration (for example a duplicate). The contact is kept. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN','EVENT_COORDINATOR')")
    @Transactional
    void delete(@PathVariable UUID id) {
        if (!registrations.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        registrations.deleteById(id);
    }
}
