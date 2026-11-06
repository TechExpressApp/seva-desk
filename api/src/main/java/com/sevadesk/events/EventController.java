package com.sevadesk.events;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

/** Admin API for events. Saving a PUBLISHED event makes it visible on the public website immediately. */
@RestController
@RequestMapping("/api/events")
@PreAuthorize("hasAnyRole('ADMIN','EVENT_COORDINATOR')")
class EventController {

    record EventForm(@NotBlank @Size(max = 200) String title, @Size(max = 400) String summary, String description,
                     @NotNull Instant startsAt, Instant endsAt, @Size(max = 200) String location,
                     @Size(max = 500) String imageUrl, @Size(max = 500) String registrationUrl,
                     @NotNull Event.Status status) {}

    record EventView(UUID id, String slug, String title, String summary, String description, Instant startsAt,
                     Instant endsAt, String location, String imageUrl, String registrationUrl, Event.Status status,
                     String publicUrl) {
        static EventView of(Event e) {
            return new EventView(e.getId(), e.getSlug(), e.getTitle(), e.getSummary(), e.getDescription(),
                    e.getStartsAt(), e.getEndsAt(), e.getLocation(), e.getImageUrl(), e.getRegistrationUrl(),
                    e.getStatus(), "/events/" + e.getSlug());
        }
    }

    private final EventRepository events;
    private final EventService service;

    EventController(EventRepository events, EventService service) {
        this.events = events;
        this.service = service;
    }

    @GetMapping
    Page<EventView> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return events.findAllByOrderByStartsAtDesc(PageRequest.of(page, Math.min(size, 100))).map(EventView::of);
    }

    @GetMapping("/{id}")
    EventView get(@PathVariable UUID id) {
        return EventView.of(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    EventView create(@Valid @RequestBody EventForm f) {
        validate(f);
        var e = new Event(service.uniqueSlug(f.title(), f.startsAt()));
        apply(e, f);
        return EventView.of(events.save(e));
    }

    @PutMapping("/{id}")
    @Transactional
    EventView update(@PathVariable UUID id, @Valid @RequestBody EventForm f) {
        validate(f);
        var e = find(id);
        apply(e, f);
        return EventView.of(e);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void delete(@PathVariable UUID id) {
        events.delete(find(id));
    }

    private Event find(UUID id) {
        return events.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    private static void validate(EventForm f) {
        if (f.endsAt() != null && f.endsAt().isBefore(f.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The end time must be after the start time");
        }
    }

    private static void apply(Event e, EventForm f) {
        e.setTitle(f.title().trim());
        e.setSummary(f.summary());
        e.setDescription(f.description());
        e.setStartsAt(f.startsAt());
        e.setEndsAt(f.endsAt());
        e.setLocation(f.location());
        e.setImageUrl(f.imageUrl());
        e.setRegistrationUrl(f.registrationUrl());
        e.setStatus(f.status());
    }
}
