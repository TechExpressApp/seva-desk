package com.sevadesk.contacts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/contacts")
class ContactController {

    record ContactForm(@NotBlank @Size(max = 100) String firstName, @Size(max = 100) String lastName,
                       @Email String email, @Size(max = 30) String phone, String city,
                       String gotra, String nakshatra, LocalDate birthDate,
                       Map<String, Object> customFields, boolean consentEmail, boolean consentWhatsapp) {}

    record ContactView(UUID id, String firstName, String lastName, String fullName, String email, String phone,
                       String city, String gotra, String nakshatra, LocalDate birthDate,
                       Map<String, Object> customFields, boolean consentEmail, boolean consentWhatsapp) {
        static ContactView of(Contact c) {
            return new ContactView(c.getId(), c.getFirstName(), c.getLastName(), c.getFullName(), c.getEmail(),
                    c.getPhone(), c.getCity(), c.getGotra(), c.getNakshatra(), c.getBirthDate(),
                    c.getCustomFields(), c.isConsentEmail(), c.isConsentWhatsapp());
        }
    }

    private final ContactRepository contacts;

    ContactController(ContactRepository contacts) {
        this.contacts = contacts;
    }

    @GetMapping
    Page<ContactView> list(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size) {
        var pageable = PageRequest.of(page, Math.min(size, 100), Sort.by("firstName", "lastName"));
        var result = q.isBlank() ? contacts.findAll(pageable) : contacts.search(q.trim(), pageable);
        return result.map(ContactView::of);
    }

    @GetMapping("/{id}")
    ContactView get(@PathVariable UUID id) {
        return ContactView.of(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ContactView create(@Valid @RequestBody ContactForm form) {
        var c = new Contact(form.firstName(), form.lastName());
        apply(c, form);
        return ContactView.of(contacts.save(c));
    }

    @PutMapping("/{id}")
    @Transactional
    ContactView update(@PathVariable UUID id, @Valid @RequestBody ContactForm form) {
        var c = find(id);
        apply(c, form);
        return ContactView.of(c);
    }

    private Contact find(UUID id) {
        return contacts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact not found"));
    }

    private static void apply(Contact c, ContactForm f) {
        c.setFirstName(f.firstName());
        c.setLastName(f.lastName());
        c.setEmail(f.email());
        c.setPhone(f.phone());
        c.setCity(f.city());
        c.setGotra(f.gotra());
        c.setNakshatra(f.nakshatra());
        c.setBirthDate(f.birthDate());
        c.setCustomFields(f.customFields());
        c.setConsentEmail(f.consentEmail());
        c.setConsentWhatsapp(f.consentWhatsapp());
    }
}
