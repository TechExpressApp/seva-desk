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

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Staff logins: roles per module, temporary passwords, forgot/reset links, deactivation ending sessions. */
class UserManagementTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @MockitoBean Mailer mailer; // off by default (temporary passwords); tests switch it on for links

    String admin;

    @BeforeEach
    void admin() throws Exception {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> {
            if (users.findByEmailIgnoreCase("users-admin@test.local").isEmpty()) {
                users.save(new AppUser("users-admin@test.local", encoder.encode("admin-password-1"), "Admin", List.of("ADMIN")));
            }
        });
        admin = login("users-admin@test.local", "admin-password-1");
    }

    @Test
    void newUserGetsATemporaryPasswordThatMustBeChangedAndOnlyTheirModules() throws Exception {
        String email = unique("treasurer");
        JsonNode created = call(post("/api/users"), admin, """
                {"displayName":"Radha Treasurer","email":"%s","roles":["TREASURER"]}""".formatted(email), 201);
        assertThat(created.get("emailSent").asBoolean()).isFalse();
        String temporary = created.get("temporaryPassword").asText();
        assertThat(temporary).hasSizeGreaterThanOrEqualTo(14);

        String token = login(email, temporary);
        JsonNode me = call(get("/api/auth/me"), token, null, 200);
        assertThat(me.get("mustChangePassword").asBoolean()).isTrue();
        assertThat(me.get("modules").toString()).contains("donations", "donors", "finance").doesNotContain("events", "users");

        // nothing else works until the temporary password is replaced
        call(get("/api/donations"), token, null, 403);
        call(post("/api/auth/change-password"), token,
                "{\"currentPassword\":\"%s\",\"newPassword\":\"short\"}".formatted(temporary), 400);
        token = cookie(mvc.perform(post("/api/auth/change-password").with(host(SITE)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"%s\",\"newPassword\":\"hare-krishna-108\"}".formatted(temporary)))
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE));

        call(get("/api/donations"), token, null, 200);
        call(get("/api/donors"), token, null, 200);
        call(get("/api/events"), token, null, 403);   // not a treasurer's module
        call(get("/api/users"), token, null, 403);    // admins only
        call(post("/api/auth/login"), null, "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, temporary), 401);
    }

    @Test
    void deactivatingOrChangingRolesEndsExistingSessions() throws Exception {
        String email = unique("frontdesk");
        String id = readyUser(email, "FRONT_DESK", "gopinath-seva-1");
        String token = login(email, "gopinath-seva-1");
        call(get("/api/contacts"), token, null, 200);

        call(put("/api/users/" + id), admin, """
                {"displayName":"Desk","roles":["FRONT_DESK","EVENT_COORDINATOR"],"active":true}""", 200);
        call(get("/api/contacts"), token, null, 401); // roles changed: sign in again
        token = login(email, "gopinath-seva-1");
        call(get("/api/events"), token, null, 200);   // new role works after signing in

        call(put("/api/users/" + id), admin, """
                {"displayName":"Desk","roles":["FRONT_DESK"],"active":false}""", 200);
        call(get("/api/contacts"), token, null, 401);
        call(post("/api/auth/login"), null, "{\"email\":\"%s\",\"password\":\"gopinath-seva-1\"}".formatted(email), 401);
    }

    @Test
    void adminsCantLockThemselvesOut() throws Exception {
        String myId = call(get("/api/users"), admin, null, 200).findValues("email").stream()
                .filter(e -> e.asText().equals("users-admin@test.local")).findFirst()
                .map(e -> "").orElse("");
        JsonNode list = call(get("/api/users"), admin, null, 200);
        for (JsonNode u : list) if (u.get("email").asText().equals("users-admin@test.local")) myId = u.get("id").asText();

        call(put("/api/users/" + myId), admin, """
                {"displayName":"Admin","roles":["ADMIN"],"active":false}""", 400);

        // With every other admin deactivated, the last one can't lose the admin role.
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        List<UUID> others = TenantContext.runAs(t.toRef(), () -> users.findAll().stream()
                .filter(u -> u.isActive() && u.getRoles().contains("ADMIN") && !u.getEmail().equals("users-admin@test.local"))
                .map(AppUser::getId).toList());
        setActive(others, false);
        try {
            JsonNode res = call(put("/api/users/" + myId), admin, """
                    {"displayName":"Admin","roles":["TREASURER"],"active":true}""", 400);
            assertThat(res.get("detail").asText()).contains("only active administrator");
        } finally {
            setActive(others, true);
        }
    }

    @Test
    void forgotPasswordEmailsALinkThatWorksOnce() throws Exception {
        String email = unique("forgot");
        readyUser(email, "FRONT_DESK", "first-password-1"); // created with email off: no invitation sent
        when(mailer.enabled()).thenReturn(true);
        when(mailer.send(anyString(), anyString(), anyString())).thenReturn(true);

        call(post("/api/auth/forgot-password"), null, "{\"email\":\"nobody-%s@example.com\"}".formatted(UUID.randomUUID()), 202);
        call(post("/api/auth/forgot-password"), null, "{\"email\":\"%s\"}".formatted(email.toUpperCase()), 202);

        var body = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(email), anyString(), body.capture());
        verify(mailer, never()).send(org.mockito.ArgumentMatchers.startsWith("nobody-"), anyString(), anyString());
        var m = Pattern.compile("/admin/reset-password\\?token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(m.find()).isTrue();
        String link = m.group(1);

        call(post("/api/auth/reset-password"), null, "{\"token\":\"%s\",\"newPassword\":\"aaaaaaaaaaaa\"}".formatted(link), 400);
        call(post("/api/auth/reset-password"), null, "{\"token\":\"%s\",\"newPassword\":\"new-password-108\"}".formatted(link), 204);
        login(email, "new-password-108");
        call(post("/api/auth/reset-password"), null, "{\"token\":\"%s\",\"newPassword\":\"another-pass-108\"}".formatted(link), 400);
    }

    @Test
    void withEmailSetUpNewUsersGetAnInvitationLinkInsteadOfATemporaryPassword() throws Exception {
        when(mailer.enabled()).thenReturn(true);
        when(mailer.send(anyString(), anyString(), anyString())).thenReturn(true);
        String email = unique("invite");
        JsonNode created = call(post("/api/users"), admin, """
                {"displayName":"Invited Volunteer","email":"%s","roles":["VOLUNTEER_COORDINATOR"]}""".formatted(email), 201);
        assertThat(created.get("emailSent").asBoolean()).isTrue();
        assertThat(created.get("temporaryPassword").isNull()).isTrue();

        var body = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(email), org.mockito.ArgumentMatchers.contains("SevaDesk login"), body.capture());
        var m = Pattern.compile("token=([A-Za-z0-9_-]+)").matcher(body.getValue());
        assertThat(m.find()).isTrue();
        call(post("/api/auth/reset-password"), null, "{\"token\":\"%s\",\"newPassword\":\"volunteer-pass-1\"}".formatted(m.group(1)), 204);
        JsonNode me = call(get("/api/auth/me"), login(email, "volunteer-pass-1"), null, 200);
        assertThat(me.get("mustChangePassword").asBoolean()).isFalse();
    }

    @Test
    void unknownRolesAndDuplicateEmailsAreRefused() throws Exception {
        call(post("/api/users"), admin, """
                {"displayName":"X","email":"%s","roles":["SUPERUSER"]}""".formatted(unique("x")), 400);
        call(post("/api/users"), admin, """
                {"displayName":"Dup","email":"USERS-ADMIN@test.local","roles":["FRONT_DESK"]}""", 409);
    }

    // ---- helpers ----

    /** A user who has already replaced their temporary password. @return their id */
    private String readyUser(String email, String role, String password) throws Exception {
        JsonNode created = call(post("/api/users"), admin, """
                {"displayName":"Test","email":"%s","roles":["%s"]}""".formatted(email, role), 201);
        String temporary = created.get("temporaryPassword").asText(null);
        if (temporary == null) { // email on: set it directly for the test
            var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
            TenantContext.runAs(t.toRef(), () -> {
                var u = users.findByEmailIgnoreCase(email).orElseThrow();
                u.changePassword(encoder.encode(password), false);
                users.save(u);
            });
        } else {
            call(post("/api/auth/change-password"), login(email, temporary),
                    "{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(temporary, password), 200);
        }
        return created.at("/user/id").asText();
    }

    private void setActive(List<UUID> ids, boolean active) {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> users.findAllById(ids).forEach(u -> {
            u.setActive(active);
            users.save(u);
        }));
    }

    private String unique(String who) {
        return who + "-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    private String login(String email, String password) throws Exception {
        var res = mvc.perform(post("/api/auth/login").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password))).andReturn().getResponse();
        assertThat(res.getStatus()).as("login " + email).isEqualTo(200);
        return cookie(res.getHeader(HttpHeaders.SET_COOKIE));
    }

    private static String cookie(String setCookie) {
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private JsonNode call(MockHttpServletRequestBuilder req, String token, String body, int expectedStatus) throws Exception {
        req.with(host(SITE));
        if (token != null) req.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        var res = mvc.perform(req).andReturn().getResponse();
        assertThat(res.getStatus()).as(req.toString() + " → " + res.getContentAsString()).isEqualTo(expectedStatus);
        String text = res.getContentAsString();
        return text.isBlank() ? json.nullNode() : json.readTree(text);
    }
}
