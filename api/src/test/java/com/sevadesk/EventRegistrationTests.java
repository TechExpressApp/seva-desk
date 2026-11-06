package com.sevadesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.platform.AppUser;
import com.sevadesk.platform.AppUserRepository;
import com.sevadesk.platform.Mailer;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Event registrations: saved, matched to a contact (or a new one), and emailed to the temple. */
class EventRegistrationTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";
    static final String TEMPLE_INBOX = "iskcon.somerset@gmail.com";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean Mailer mailer;

    String coordinator;
    String frontDesk;
    String treasurer;

    @BeforeEach
    void setUp() throws Exception {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> {
            for (var u : List.of(new String[]{"reg-coord@test.local", "EVENT_COORDINATOR"},
                    new String[]{"reg-desk@test.local", "FRONT_DESK"}, new String[]{"reg-treasurer@test.local", "TREASURER"})) {
                if (users.findByEmailIgnoreCase(u[0]).isEmpty()) {
                    users.save(new AppUser(u[0], encoder.encode("test-password-1"), "Tester", List.of(u[1])));
                }
            }
        });
        coordinator = login("reg-coord@test.local");
        frontDesk = login("reg-desk@test.local");
        treasurer = login("reg-treasurer@test.local");
        clearInvocations(mailer);
        when(mailer.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void registeringSavesAContactAndEmailsTheTemple() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = "radha." + tag + "@example.com";

        JsonNode res = call(post("/api/event-registrations"), frontDesk, form("Govardhan Puja " + tag, "Radha", "Sharma " + tag,
                email, "+1 (732) 555-" + tag.substring(0, 4).replaceAll("[^0-9]", "1"), "Edison", "NJ", 34), 201);
        assertThat(res.get("contact").asText()).isEqualTo("CREATED");
        assertThat(res.get("emailSent").asBoolean()).isTrue();
        JsonNode reg = res.get("registration");
        assertThat(reg.get("eventName").asText()).isEqualTo("Govardhan Puja " + tag);
        assertThat(reg.get("age").asInt()).isEqualTo(34);
        assertThat(reg.get("registrationDate").asText())                 // today, in the temple's time zone
                .isEqualTo(java.time.LocalDate.now(java.time.ZoneId.of("America/New_York")).toString());
        assertThat(reg.get("registeredAt").asText()).isNotBlank();
        String contactId = reg.get("contactId").asText();

        // the contact holds the details
        JsonNode contact = call(get("/api/contacts/" + contactId), frontDesk, null, 200);
        assertThat(contact.get("email").asText()).isEqualTo(email);
        assertThat(contact.get("city").asText()).isEqualTo("Edison");
        assertThat(contact.at("/customFields/state").asText()).isEqualTo("NJ");
        assertThat(contact.at("/customFields/age").asInt()).isEqualTo(34);
        assertThat(contact.at("/customFields/source").asText()).isEqualTo("Event registration: Govardhan Puja " + tag);

        // the temple is emailed with the details
        var subject = ArgumentCaptor.forClass(String.class);
        var body = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(TEMPLE_INBOX), subject.capture(), body.capture());
        assertThat(subject.getValue()).isEqualTo("New registration: Govardhan Puja " + tag + " – Radha Sharma " + tag);
        assertThat(body.getValue()).contains("Radha Sharma " + tag, email, "Edison", "NJ", "34", "New contact added");
    }

    @Test
    void theSamePersonRegisteringAgainIsMatchedNotDuplicated() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = "madhav." + tag + "@example.com";
        String first = call(post("/api/event-registrations"), frontDesk,
                form("Gita Jayanti " + tag, "Madhav", "Rao", email, "732-555-0101", "Somerset", "NJ", 41), 201)
                .at("/registration/contactId").asText();

        JsonNode again = call(post("/api/event-registrations"), frontDesk,
                form("Sunday Love Feast " + tag, "Madhav", "Rao", email.toUpperCase(), "732-555-0101", "Somerset", "NJ", 41), 201);
        assertThat(again.get("contact").asText()).isEqualTo("EMAIL");
        assertThat(again.at("/registration/contactId").asText()).isEqualTo(first);

        JsonNode mine = call(get("/api/event-registrations").param("contactId", first), frontDesk, null, 200);
        assertThat(mine.at("/page/totalElements").asLong()).isEqualTo(2);
    }

    @Test
    void familyMembersSharingAWhatsappNumberGetTheirOwnContacts() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String phone = "+1 908 555 " + String.format("%04d", Math.abs(tag.hashCode()) % 10000);
        String parent = call(post("/api/event-registrations"), frontDesk,
                form("Kids Camp " + tag, "Lakshmi", "Iyer " + tag, "lakshmi." + tag + "@example.com", phone, "Edison", "NJ", 38), 201)
                .at("/registration/contactId").asText();

        // her daughter, same WhatsApp number, own email: a separate contact
        JsonNode child = call(post("/api/event-registrations"), frontDesk,
                form("Kids Camp " + tag, "Tulasi", "Iyer " + tag, "tulasi." + tag + "@example.com", phone, "Edison", "NJ", 9), 201);
        assertThat(child.get("contact").asText()).isEqualTo("CREATED");
        assertThat(child.at("/registration/contactId").asText()).isNotEqualTo(parent);

        // the mother again, without email: matched by number because the first name agrees
        JsonNode again = call(post("/api/event-registrations"), frontDesk,
                form("Gita Class " + tag, "lakshmi", "Iyer " + tag, "lakshmi.other." + tag + "@example.com", phone, "Edison", "NJ", 38), 201);
        assertThat(again.get("contact").asText()).isEqualTo("PHONE");
        assertThat(again.at("/registration/contactId").asText()).isEqualTo(parent);
    }

    @Test
    void aRegistrationIsLinkedToTheEventWithThatName() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String title = "Janmashtami Festival " + tag;
        String eventId = call(post("/api/events"), coordinator, """
                {"title":"%s","startsAt":"%s","status":"PUBLISHED"}""".formatted(title, Instant.now().plus(30, ChronoUnit.DAYS)), 201)
                .get("id").asText();

        JsonNode reg = call(post("/api/event-registrations"), coordinator,
                form(title.toLowerCase(), "Gopal", "Das", "gopal." + tag + "@example.com", "7325550199", "Piscataway", "NJ", 12), 201)
                .get("registration");
        assertThat(reg.get("eventId").asText()).isEqualTo(eventId);
        assertThat(reg.get("eventName").asText()).isEqualTo(title);      // the event's own spelling

        // the form offers the upcoming event, to front desk too
        JsonNode options = call(get("/api/event-registrations/event-options"), frontDesk, null, 200);
        assertThat(options.toString()).contains(title);

        // and the screen is open to the people who can use it
        assertThat(call(get("/api/auth/me"), frontDesk, null, 200).get("modules").toString()).contains("registrations");
        assertThat(call(get("/api/auth/me"), coordinator, null, 200).get("modules").toString()).contains("registrations");
        assertThat(call(get("/api/auth/me"), treasurer, null, 200).get("modules").toString()).doesNotContain("registrations");

        JsonNode list = call(get("/api/event-registrations").param("eventName", title.toUpperCase()), coordinator, null, 200);
        assertThat(list.at("/page/totalElements").asLong()).isEqualTo(1);
        String id = list.at("/content/0/id").asText();

        call(delete("/api/event-registrations/" + id), frontDesk, null, 403);   // front desk can't delete
        call(delete("/api/event-registrations/" + id), coordinator, null, 204);
        call(get("/api/event-registrations/" + id), coordinator, null, 404);
    }

    @Test
    void everyFieldIsRequiredAndChecked() throws Exception {
        call(post("/api/event-registrations"), frontDesk, form("", "A", "B", "a@b.com", "7325550100", "X", "NJ", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "", "B", "a@b.com", "7325550100", "X", "NJ", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "A", "B", "not-an-email", "7325550100", "X", "NJ", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "A", "B", "a@b.com", "call me", "X", "NJ", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "A", "B", "a@b.com", "7325550100", "", "NJ", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "A", "B", "a@b.com", "7325550100", "X", "", 20), 400);
        call(post("/api/event-registrations"), frontDesk, form("Feast", "A", "B", "a@b.com", "7325550100", "X", "NJ", 130), 400);
        call(post("/api/event-registrations"), frontDesk, """
                {"eventName":"Feast","firstName":"A","lastName":"B","email":"a@b.com","whatsapp":"7325550100","city":"X","state":"NJ"}""", 400);
    }

    @Test
    void onlyStaffWhoHandleEventsCanUseIt() throws Exception {
        call(post("/api/event-registrations"), treasurer, form("Feast", "A", "B", "a@b.com", "7325550100", "X", "NJ", 20), 403);
        call(get("/api/event-registrations"), treasurer, null, 403);
        var res = mvc.perform(post("/api/event-registrations").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                .content(form("Feast", "A", "B", "a@b.com", "7325550100", "X", "NJ", 20))).andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(401);
    }

    private static String form(String event, String first, String last, String email, String whatsapp, String city,
                               String state, int age) {
        return """
                {"eventName":"%s","firstName":"%s","lastName":"%s","email":"%s","whatsapp":"%s","city":"%s","state":"%s","age":%d}"""
                .formatted(event, first, last, email, whatsapp, city, state, age);
    }

    private String login(String email) throws Exception {
        var res = mvc.perform(post("/api/auth/login").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"test-password-1\"}".formatted(email))).andReturn().getResponse();
        assertThat(res.getStatus()).isEqualTo(200);
        String c = res.getHeader(HttpHeaders.SET_COOKIE);
        return c.substring(c.indexOf('=') + 1, c.indexOf(';'));
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, String body, int expected) throws Exception {
        req.with(host(SITE)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        var res = mvc.perform(req).andReturn().getResponse();
        assertThat(res.getStatus()).as(req + " → " + res.getContentAsString()).isEqualTo(expected);
        String text = res.getContentAsString();
        return text.isBlank() ? json.nullNode() : json.readTree(text);
    }
}
