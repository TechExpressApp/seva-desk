package com.sevadesk.platform;

import java.util.List;

/**
 * What a staff login can use. A user can have several roles. Admin can do everything, including managing users.
 * The admin app shows only the modules a user's roles allow, and the API enforces the same rules.
 */
public enum Role {
    ADMIN("Administrator", "Everything, including users and settings",
            List.of("dashboard", "contacts", "donations", "donors", "events", "finance", "seva", "guesthouse",
                    "members", "volunteers", "users")),
    TREASURER("Treasurer", "Donations (including voiding), donors, expenses and finance reports",
            List.of("dashboard", "contacts", "donations", "donors", "finance", "members")),
    FRONT_DESK("Front desk", "Contacts and donation entry",
            List.of("dashboard", "contacts", "donations", "donors")),
    EVENT_COORDINATOR("Event coordinator", "Events and the public website's event listings",
            List.of("contacts", "events")),
    PRIEST("Priest services", "Seva bookings and priest schedules", List.of("seva")),
    GUEST_HOUSE("Guest house", "Rooms, bookings and check-ins", List.of("guesthouse")),
    VOLUNTEER_COORDINATOR("Volunteer coordinator", "Volunteers and shifts", List.of("contacts", "volunteers"));

    private final String label;
    private final String description;
    private final List<String> modules;

    Role(String label, String description, List<String> modules) {
        this.label = label;
        this.description = description;
        this.modules = modules;
    }

    public String label() { return label; }
    public String description() { return description; }
    public List<String> modules() { return modules; }

    public static boolean isValid(String name) {
        for (Role r : values()) if (r.name().equals(name)) return true;
        return false;
    }
}
