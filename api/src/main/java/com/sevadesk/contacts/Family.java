package com.sevadesk.contacts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A devotee's spouse and up to three children, with birthdays for greetings and sankalpam.
 * Stored inside the contact's custom_fields under "family":
 * {"spouse": {"name": "...", "birthDate": "1985-04-12"}, "children": [{"name": "...", "birthDate": "..."}]}
 */
public record Family(@Valid Member spouse, @Valid @Size(max = 3) List<Member> children) {

    public static final int MAX_CHILDREN = 3;

    public record Member(@Size(max = 100) String name, LocalDate birthDate) {
        boolean isEmpty() {
            return (name == null || name.isBlank()) && birthDate == null;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            if (name != null && !name.isBlank()) m.put("name", name.trim());
            if (birthDate != null) m.put("birthDate", birthDate.toString());
            return m;
        }

        static Member fromMap(Object o) {
            if (!(o instanceof Map<?, ?> m)) return null;
            Object name = m.get("name");
            Object date = m.get("birthDate");
            LocalDate birthDate = null;
            if (date instanceof String s && !s.isBlank()) {
                try {
                    birthDate = LocalDate.parse(s);
                } catch (DateTimeParseException ignored) {
                    // tolerate hand-edited data
                }
            }
            var member = new Member(name instanceof String s ? s : null, birthDate);
            return member.isEmpty() ? null : member;
        }
    }

    public Family {
        if (spouse != null && spouse.isEmpty()) spouse = null;
        children = children == null ? List.of()
                : children.stream().filter(c -> c != null && !c.isEmpty()).limit(MAX_CHILDREN).toList();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return spouse == null && children.isEmpty();
    }

    Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (spouse != null) m.put("spouse", spouse.toMap());
        if (!children.isEmpty()) m.put("children", children.stream().map(Member::toMap).toList());
        return m;
    }

    static Family fromMap(Object o) {
        if (!(o instanceof Map<?, ?> m)) return new Family(null, List.of());
        List<Member> kids = new ArrayList<>();
        if (m.get("children") instanceof List<?> list) {
            for (Object c : list) {
                Member member = Member.fromMap(c);
                if (member != null) kids.add(member);
            }
        }
        return new Family(Member.fromMap(m.get("spouse")), kids);
    }
}
