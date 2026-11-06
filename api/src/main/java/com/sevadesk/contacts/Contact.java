package com.sevadesk.contacts;

import com.sevadesk.platform.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/** A devotee, donor, member or volunteer. One person, many roles. */
@Entity
@Table(name = "contact")
public class Contact extends TenantScopedEntity {

    @Column(name = "first_name") private String firstName;
    @Column(name = "last_name") private String lastName;
    private String email;
    private String phone;
    private String city;
    private String gotra;
    private String nakshatra;
    @Column(name = "birth_date") private LocalDate birthDate;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields") private Map<String, Object> customFields = new HashMap<>();
    @Column(name = "consent_email") private boolean consentEmail;
    @Column(name = "consent_whatsapp") private boolean consentWhatsapp;

    protected Contact() {}

    public Contact(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getCity() { return city; }
    public void setCity(String v) { city = v; }
    public String getGotra() { return gotra; }
    public void setGotra(String v) { gotra = v; }
    public String getNakshatra() { return nakshatra; }
    public void setNakshatra(String v) { nakshatra = v; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate v) { birthDate = v; }
    public Map<String, Object> getCustomFields() { return customFields; }
    public void setCustomFields(Map<String, Object> v) { customFields = v == null ? new HashMap<>() : v; }
    public boolean isConsentEmail() { return consentEmail; }
    public void setConsentEmail(boolean v) { consentEmail = v; }
    public boolean isConsentWhatsapp() { return consentWhatsapp; }
    public void setConsentWhatsapp(boolean v) { consentWhatsapp = v; }

    public String getFullName() {
        return lastName == null || lastName.isBlank() ? firstName : firstName + " " + lastName;
    }
}
