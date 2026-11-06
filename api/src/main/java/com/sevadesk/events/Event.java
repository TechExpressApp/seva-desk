package com.sevadesk.events;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

/** A festival, programme or class. PUBLISHED events appear on the temple's public website straight away. */
@Entity
@Table(name = "event")
public class Event extends TenantScopedEntity {

    public enum Status { DRAFT, PUBLISHED, CANCELLED }

    private String slug;
    private String title;
    private String summary;
    private String description;
    @Column(name = "starts_at") private Instant startsAt;
    @Column(name = "ends_at") private Instant endsAt;
    private String location;
    @Column(name = "image_url") private String imageUrl;
    @Column(name = "registration_url") private String registrationUrl;
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    protected Event() {}

    public Event(String slug) {
        this.slug = slug;
    }

    public String getSlug() { return slug; }
    public void setSlug(String v) { slug = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { title = v; }
    public String getSummary() { return summary; }
    public void setSummary(String v) { summary = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { description = v; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant v) { startsAt = v; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant v) { endsAt = v; }
    public String getLocation() { return location; }
    public void setLocation(String v) { location = v; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String v) { imageUrl = v; }
    public String getRegistrationUrl() { return registrationUrl; }
    public void setRegistrationUrl(String v) { registrationUrl = v; }
    public Status getStatus() { return status; }
    public void setStatus(Status v) { status = v; }
}
