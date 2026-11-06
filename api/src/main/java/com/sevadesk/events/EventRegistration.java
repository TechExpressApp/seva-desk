package com.sevadesk.events;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/** One person's registration for an event. The person is also kept as a contact (contactId). */
@Entity
@Table(name = "event_registration")
public class EventRegistration extends TenantScopedEntity {

    @Column(name = "event_id") private UUID eventId;
    @Column(name = "event_name") private String eventName;
    @Column(name = "contact_id") private UUID contactId;
    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    private String email;
    private String whatsapp;
    private String city;
    private String state;
    private int age;

    protected EventRegistration() {}

    public EventRegistration(UUID eventId, String eventName, UUID contactId, String firstName, String lastName,
                             String email, String whatsapp, String city, String state, int age) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.contactId = contactId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.whatsapp = whatsapp;
        this.city = city;
        this.state = state;
        this.age = age;
    }

    public UUID getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public UUID getContactId() { return contactId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getWhatsapp() { return whatsapp; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public int getAge() { return age; }
}
