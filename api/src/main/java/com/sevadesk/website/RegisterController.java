package com.sevadesk.website;

import com.sevadesk.events.EventRegistrationService;
import com.sevadesk.events.EventRegistrationService.NewRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Public event registration page (/register?event=...), opened from "Join us" on the home page and from event
 * pages. Uses the same registration as the admin: saved, matched to a contact (or a new one), emailed to the temple.
 */
@Controller
class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()\\-.]{7,40}$");

    private final EventRegistrationService registrations;
    private final RegistrationFormModel form;

    RegisterController(EventRegistrationService registrations, RegistrationFormModel form) {
        this.registrations = registrations;
        this.form = form;
    }

    @GetMapping("/register")
    String form(@RequestParam(required = false) String event, Model model) {
        form.prepare(model, clean(event, 200));
        return "site/register";
    }

    @PostMapping("/register")
    String submit(@RequestParam Map<String, String> params, Model model, RedirectAttributes redirect) {
        if (!blank(params.get("website"))) return "redirect:/register/thank-you"; // spam trap: bots fill it in
        Map<String, String> v = new LinkedHashMap<>();
        for (String f : RegistrationFormModel.FIELDS) {
            v.put(f, clean(params.get(f), f.equals("eventName") ? 200 : f.equals("email") ? 254 : 100));
        }
        String error = validate(v);
        if (error != null) {
            form.prepare(model, v, error);
            return "site/register";
        }

        var result = registrations.register(new NewRegistration(v.get("eventName"), v.get("firstName"),
                v.get("lastName"), v.get("email"), v.get("whatsapp"), v.get("city"), v.get("state"),
                Integer.parseInt(v.get("age"))));
        registrations.notifyTemple(result);                     // after the registration is saved
        log.info("Website registration for '{}'", result.registration().getEventName());
        redirect.addAttribute("event", result.registration().getEventName());
        redirect.addAttribute("name", result.registration().getFirstName());
        return "redirect:/register/thank-you";
    }

    @GetMapping("/register/thank-you")
    String thanks(@RequestParam(required = false) String event, @RequestParam(required = false) String name, Model model) {
        model.addAttribute("event", clean(event, 200));
        model.addAttribute("name", clean(name, 100));
        return "site/register-thanks";
    }


    private static String validate(Map<String, String> v) {
        if (blank(v.get("eventName"))) return "Please choose the event you'd like to join.";
        if (blank(v.get("firstName")) || blank(v.get("lastName"))) return "Please enter your first and last name.";
        if (blank(v.get("email")) || !EMAIL.matcher(v.get("email")).matches()) return "Please enter a valid email address.";
        if (blank(v.get("whatsapp")) || !PHONE.matcher(v.get("whatsapp")).matches())
            return "Please enter your WhatsApp number, with the country code (for example +1 732 555 0100).";
        if (blank(v.get("city")) || blank(v.get("state"))) return "Please enter your city and state.";
        try {
            int age = Integer.parseInt(v.get("age"));
            if (age < 0 || age > 120) return "Please enter an age between 0 and 120.";
        } catch (NumberFormatException e) {
            return "Please enter your age.";
        }
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
}
