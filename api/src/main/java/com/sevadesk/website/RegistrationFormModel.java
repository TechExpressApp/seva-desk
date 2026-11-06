package com.sevadesk.website;

import com.sevadesk.events.EventService;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * What the event registration form needs: the event choices and the values to show. Used by the /register page
 * and by any website page that has the form at the bottom (pages with "registerEvent" set).
 */
@Component
class RegistrationFormModel {

    static final List<String> FIELDS =
            List.of("eventName", "firstName", "lastName", "email", "whatsapp", "city", "state", "age");

    private final EventService events;

    RegistrationFormModel(EventService events) {
        this.events = events;
    }

    /** An empty form with this event chosen (null = none chosen). */
    void prepare(Model model, String event) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String f : FIELDS) values.put(f, null);           // every field present, so the page can show them
        values.put("eventName", event);
        prepare(model, values, null);
    }

    /** The form with what the visitor typed and, if something was wrong, why. */
    void prepare(Model model, Map<String, String> values, String error) {
        // event choices: the home page's weekly gatherings, then upcoming events, plus the one asked for
        var site = (SiteSettings) model.getAttribute("site");
        var names = new LinkedHashSet<String>();
        if (site != null && site.gatherings() != null) site.gatherings().items().forEach(g -> names.add(g.title()));
        events.upcomingPublished(50).forEach(e -> names.add(e.getTitle()));
        String wanted = values.get("eventName");
        if (wanted != null) {
            var known = names.stream().filter(n -> n.equalsIgnoreCase(wanted)).findFirst();
            if (known.isPresent()) values.put("eventName", known.get());   // use the list's spelling
            else names.add(wanted);
        }
        model.addAttribute("eventNames", new ArrayList<>(names));
        model.addAttribute("v", values);
        model.addAttribute("error", error);
    }
}
