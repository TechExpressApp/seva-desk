package com.sevadesk.events;

import com.sevadesk.platform.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Used by the admin API and by the public website module. */
@Service
public class EventService {

    private final EventRepository events;

    EventService(EventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Event> upcomingPublished(int limit) {
        return events.upcoming(Instant.now(), PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public Optional<Event> publishedBySlug(String slug) {
        return events.findBySlugAndStatus(slug, Event.Status.PUBLISHED);
    }

    /** "Govardhan Puja & Annakut" on 8 Nov 2026 → "govardhan-puja-annakut-2026-11-08", made unique per temple. */
    @Transactional(readOnly = true)
    public String uniqueSlug(String title, Instant startsAt) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (base.isEmpty()) base = "event";
        if (base.length() > 90) base = base.substring(0, 90).replaceAll("-$", "");
        // date in the temple's own time zone: an 8 pm New York event keeps its New York date
        base = base + "-" + startsAt.atZone(TenantContext.require().zone()).toLocalDate();
        String slug = base;
        for (int i = 2; events.existsBySlug(slug); i++) slug = base + "-" + i;
        return slug;
    }
}
