package com.sevadesk.website;

import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.events.EventRegistrationService;
import com.sevadesk.events.EventRegistrationService.NewRegistration;
import com.sevadesk.events.EventRegistrationService.Registered;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Course sign-up form on website pages with "courseSignup" set (e.g. /bhagavad-gita-course). The person is saved as
 * a contact (matched to an existing one when possible) with their address and preferred language, recorded as a
 * registration for the course so staff see it under Registrations, and then sent to that language's WhatsApp group.
 */
@Controller
class CourseSignupController {

    static final List<String> FIELDS = List.of("firstName", "lastName", "age", "addressLine1", "addressLine2", "city",
            "state", "zip", "whatsapp", "email", "language");

    private static final Logger log = LoggerFactory.getLogger(CourseSignupController.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()\\-.]{7,40}$");
    private static final Pattern ZIP = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9 -]{2,9}$");

    private final CourseSignupService signups;
    private final EventRegistrationService registrations;

    CourseSignupController(CourseSignupService signups, EventRegistrationService registrations) {
        this.signups = signups;
        this.registrations = registrations;
    }

    @PostMapping("/course-signup")
    String submit(@RequestParam Map<String, String> params, Model model, RedirectAttributes redirect,
                  HttpServletResponse response) {
        String pageKey = clean(params.get("page"), 100);
        var page = pageKey == null ? null : site(model).pages().get(pageKey);
        if (page == null || page.courseSignup() == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "site/not-found";
        }
        var course = page.courseSignup();
        if (!blank(params.get("website"))) return "redirect:/" + pageKey;   // spam trap: bots fill it in

        Map<String, String> v = new LinkedHashMap<>();
        for (String f : FIELDS) v.put(f, clean(params.get(f), f.equals("email") ? 254 : f.startsWith("address") ? 200 : 100));
        String error = validate(v, course);
        if (error != null) {
            model.addAttribute("page", page);
            model.addAttribute("pageKey", pageKey);
            prepare(model, v, error);
            return "site/page";
        }

        var language = course.language(v.get("language")).orElseThrow();
        var result = signups.signUp(course.course(), language.name(), v);
        registrations.notifyTemple(result);                     // after the sign-up is saved
        log.info("Course sign-up for '{}' ({})", course.course(), language.name());

        redirect.addAttribute("page", pageKey);
        redirect.addAttribute("language", language.name());
        redirect.addAttribute("name", result.registration().getFirstName());
        return "redirect:/course-signup/joined";
    }

    /** Thank-you page that forwards to the WhatsApp group. The link comes from the site settings, never the URL. */
    @GetMapping("/course-signup/joined")
    String joined(@RequestParam(name = "page", required = false) String pageKey,
                  @RequestParam(required = false) String language, @RequestParam(required = false) String name,
                  Model model, HttpServletResponse response) {
        var page = pageKey == null ? null : site(model).pages().get(pageKey);
        var lang = page == null || page.courseSignup() == null ? null : page.courseSignup().language(language).orElse(null);
        if (lang == null) {
            response.setStatus(HttpStatus.NOT_FOUND.value());
            return "site/not-found";
        }
        model.addAttribute("page", page);
        model.addAttribute("pageKey", pageKey);
        model.addAttribute("course", page.courseSignup());
        model.addAttribute("language", lang);
        model.addAttribute("name", clean(name, 100));
        return "site/course-joined";
    }

    /** An empty form; used by the page itself. */
    static void prepare(Model model) {
        Map<String, String> v = new LinkedHashMap<>();
        for (String f : FIELDS) v.put(f, null);
        prepare(model, v, null);
    }

    private static void prepare(Model model, Map<String, String> values, String error) {
        model.addAttribute("cv", values);
        model.addAttribute("courseError", error);
    }

    private static String validate(Map<String, String> v, SiteSettings.CourseSignup course) {
        if (blank(v.get("firstName")) || blank(v.get("lastName"))) return "Please enter your first and last name.";
        try {
            int age = Integer.parseInt(v.get("age"));
            if (age < 1 || age > 120) return "Please enter an age between 1 and 120.";
        } catch (NumberFormatException e) {
            return "Please enter your age.";
        }
        if (blank(v.get("addressLine1"))) return "Please enter your address.";
        if (blank(v.get("city")) || blank(v.get("state"))) return "Please enter your city and state.";
        if (blank(v.get("zip")) || !ZIP.matcher(v.get("zip")).matches()) return "Please enter a valid ZIP code.";
        if (blank(v.get("whatsapp")) || !PHONE.matcher(v.get("whatsapp")).matches())
            return "Please enter your WhatsApp number, with the country code (for example +1 732 555 0100).";
        if (blank(v.get("email")) || !EMAIL.matcher(v.get("email")).matches()) return "Please enter a valid email address.";
        if (course.language(v.get("language")).isEmpty()) return "Please choose your preferred language.";
        return null;
    }

    private static String clean(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.strip().replaceAll("\\s+", " ");
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static SiteSettings site(Model model) {
        return (SiteSettings) model.getAttribute("site");
    }
}

/** Saves a course sign-up: the registration and the contact's address and language, together or not at all. */
@Service
class CourseSignupService {

    private final EventRegistrationService registrations;
    private final ContactRepository contacts;

    CourseSignupService(EventRegistrationService registrations, ContactRepository contacts) {
        this.registrations = registrations;
        this.contacts = contacts;
    }

    @Transactional
    Registered signUp(String course, String language, Map<String, String> v) {
        var result = registrations.register(new NewRegistration(course, v.get("firstName"), v.get("lastName"),
                v.get("email"), v.get("whatsapp"), v.get("city"), v.get("state"), Integer.parseInt(v.get("age"))));
        var contact = contacts.findById(result.registration().getContactId()).orElseThrow();
        Map<String, Object> fields = new HashMap<>(contact.getCustomFields());
        // A public form must not overwrite an address staff already have; it only fills in what is missing.
        if (fields.get("addressLine1") == null) {
            fields.put("addressLine1", v.get("addressLine1"));
            if (v.get("addressLine2") != null) fields.put("addressLine2", v.get("addressLine2"));
        }
        fields.putIfAbsent("zip", v.get("zip"));
        fields.put("preferredLanguage", language);               // latest choice
        contact.setCustomFields(fields);
        contact.setConsentWhatsapp(true);                       // they are joining the course's WhatsApp group
        return result;
    }
}
