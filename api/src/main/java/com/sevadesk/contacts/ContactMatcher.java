package com.sevadesk.contacts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Finds the contact a donation belongs to, so each person's giving adds up in one place.
 * Tried in order: email (ignoring case), phone (digits only, last 10), full name (ignoring case and how it was
 * split into first/last). If several contacts match, the oldest wins. If nothing matches, a new contact is created.
 */
@Service
public class ContactMatcher {

    private static final Logger log = LoggerFactory.getLogger(ContactMatcher.class);

    public enum MatchedBy { EMAIL, PHONE, NAME, CREATED }

    /** What we know about a donor. Any field may be blank. */
    public record DonorDetails(String firstName, String lastName, String email, String phone) {
        public boolean hasAny() {
            return !blank(firstName) || !blank(lastName) || !blank(email) || !blank(phone);
        }

        /** "Gopal Krishna Das" → first "Gopal", last "Krishna Das". */
        public static DonorDetails fromFullName(String fullName, String email, String phone) {
            String name = fullName == null ? "" : fullName.strip().replaceAll("\\s+", " ");
            int space = name.indexOf(' ');
            return new DonorDetails(space > 0 ? name.substring(0, space) : name,
                    space > 0 ? name.substring(space + 1) : null, email, phone);
        }
    }

    public record Match(Contact contact, MatchedBy matchedBy) {}

    private final ContactRepository contacts;

    ContactMatcher(ContactRepository contacts) {
        this.contacts = contacts;
    }

    /** The existing contact for these details, if any (no changes made). */
    @Transactional(readOnly = true)
    public Optional<Match> find(DonorDetails d) {
        return find(d, false);
    }

    /**
     * As {@link #find(DonorDetails)}; with phoneNeedsSameFirstName, a phone match counts only when the first names
     * agree too. Use it where family members often share one number (event registrations for a whole family).
     */
    @Transactional(readOnly = true)
    public Optional<Match> find(DonorDetails d, boolean phoneNeedsSameFirstName) {
        String email = normalizeEmail(d.email());
        if (email != null) {
            var byEmail = contacts.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(email);
            if (byEmail.isPresent()) return Optional.of(new Match(byEmail.get(), MatchedBy.EMAIL));
        }
        String digits = phoneKey(d.phone());
        if (digits != null) {
            var byPhone = contacts.findFirstByPhoneDigits(digits)
                    .filter(c -> !phoneNeedsSameFirstName || sameFirstName(c.getFirstName(), d.firstName()));
            if (byPhone.isPresent()) return Optional.of(new Match(byPhone.get(), MatchedBy.PHONE));
        }
        String fullName = fullNameKey(d.firstName(), d.lastName());
        if (fullName != null) {
            var byName = contacts.findFirstByFullName(fullName);
            if (byName.isPresent()) return Optional.of(new Match(byName.get(), MatchedBy.NAME));
        }
        return Optional.empty();
    }

    /**
     * The matching contact, with any missing email/phone/last name filled in from these details,
     * or a new contact if nobody matches. Returns empty only when no details were given (anonymous gift).
     */
    @Transactional
    public Optional<Match> findOrCreate(DonorDetails d, String source) {
        return findOrCreate(d, source, false);
    }

    /** As {@link #findOrCreate(DonorDetails, String)}; see {@link #find(DonorDetails, boolean)} for the flag. */
    @Transactional
    public Optional<Match> findOrCreate(DonorDetails d, String source, boolean phoneNeedsSameFirstName) {
        if (d == null || !d.hasAny()) return Optional.empty();
        var found = find(d, phoneNeedsSameFirstName);
        if (found.isPresent()) {
            enrich(found.get().contact(), d);
            return found;
        }
        var c = new Contact(firstNameFor(d), blank(d.lastName()) ? null : d.lastName().strip());
        c.setEmail(normalizeEmail(d.email()));
        c.setPhone(blank(d.phone()) ? null : d.phone().strip());
        Map<String, Object> fields = new HashMap<>();
        fields.put("source", source);
        c.setCustomFields(fields);
        var saved = contacts.save(c);
        log.info("Created contact {} for a new donor ({})", saved.getId(), source);
        return Optional.of(new Match(saved, MatchedBy.CREATED));
    }

    private static void enrich(Contact c, DonorDetails d) {
        if (blank(c.getEmail()) && normalizeEmail(d.email()) != null) c.setEmail(normalizeEmail(d.email()));
        if (blank(c.getPhone()) && !blank(d.phone())) c.setPhone(d.phone().strip());
        if (blank(c.getLastName()) && !blank(d.lastName())
                && c.getFirstName().equalsIgnoreCase(blank(d.firstName()) ? "" : d.firstName().strip())) {
            c.setLastName(d.lastName().strip());
        }
    }

    private static String firstNameFor(DonorDetails d) {
        if (!blank(d.firstName())) return d.firstName().strip();
        String email = normalizeEmail(d.email());
        if (email != null) return email.substring(0, email.indexOf('@'));
        if (!blank(d.lastName())) return d.lastName().strip();
        return blank(d.phone()) ? "Donor" : d.phone().strip();
    }

    static String normalizeEmail(String email) {
        if (blank(email) || !email.contains("@")) return null;
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /** Digits only, last 10 (drops a +1 country code); null if fewer than 7 digits. */
    static String phoneKey(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 7) return null;
        return digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    }

    /** "  Gopal  Krishna " + "DAS" → "gopal krishna das"; null without a first name. */
    static String fullNameKey(String first, String last) {
        if (blank(first)) return null;
        String full = (first + " " + (last == null ? "" : last)).strip().replaceAll("\\s+", " ");
        return full.toLowerCase(Locale.ROOT);
    }

    private static boolean sameFirstName(String a, String b) {
        return !blank(a) && !blank(b) && a.strip().equalsIgnoreCase(b.strip());
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
