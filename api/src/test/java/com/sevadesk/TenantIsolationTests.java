package com.sevadesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.platform.AppUser;
import com.sevadesk.platform.AppUserRepository;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end check of the white-label core against real PostgreSQL: tenant from host name, login,
 * gap-free receipt numbers, tokens bound to one temple, and row-level security between temples.
 */
class TenantIsolationTests extends PostgresTestBase {

    static final String RG = "radhagovinda.localhost";
    static final String GN = "gurunanak.localhost";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createAdmins() {
        for (var t : tenants.findByActiveTrue()) {
            TenantContext.runAs(t.toRef(), () -> {
                if (users.findByEmailIgnoreCase("admin@test.local").isEmpty()) {
                    users.save(new AppUser("admin@test.local", encoder.encode("test-password"), "Admin", List.of("ADMIN")));
                }
            });
        }
    }

    @Test
    void brandingComesFromTheHostName() throws Exception {
        mvc.perform(get("/api/public/branding").with(host(RG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sri Radha Govinda Mandir"))
                .andExpect(jsonPath("$.terms['term.priest']").value("Pujari"));
        mvc.perform(get("/api/public/branding").with(host(GN)))
                .andExpect(jsonPath("$.greeting").value("Waheguru Ji Ka Khalsa"))
                .andExpect(jsonPath("$.terms['term.priest']").value("Granthi"));
        mvc.perform(get("/api/public/branding").with(host("unknown.localhost")))
                .andExpect(status().isNotFound());
    }

    @Test
    void httpsCertificatesOnlyForRegisteredTemples() throws Exception {
        mvc.perform(get("/api/public/domain-allowed").param("domain", "radhagovinda.sevadesk.app").with(host("api")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/public/domain-allowed").param("domain", "someone-else.example.com").with(host("api")))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login").with(host(RG)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void donationsGetReceiptsAndStayInsideTheirTemple() throws Exception {
        String rgToken = login(RG);

        String fundId = body(mvc.perform(authed(post("/api/funds"), RG, rgToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Annadanam\",\"taxDeductible\":true}"))
                .andExpect(status().isCreated())).get("id").asText();

        String donation = "{\"fundId\":\"" + fundId + "\",\"amount\":2501,\"paymentMode\":\"UPI\"}";
        String first = body(mvc.perform(authed(post("/api/donations"), RG, rgToken)
                .contentType(MediaType.APPLICATION_JSON).content(donation))
                .andExpect(status().isCreated())).get("receiptNo").asText();
        String second = body(mvc.perform(authed(post("/api/donations"), RG, rgToken)
                .contentType(MediaType.APPLICATION_JSON).content(donation))).get("receiptNo").asText();

        assertThat(first).matches("RG/\\d{4}-\\d{2}/\\d{5}");
        assertThat(Integer.parseInt(second.substring(second.lastIndexOf('/') + 1)))
                .isEqualTo(Integer.parseInt(first.substring(first.lastIndexOf('/') + 1)) + 1);

        mvc.perform(authed(get("/api/dashboard/summary"), RG, rgToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayCount").value(2));

        // A Radha Govinda token is useless on the Gurdwara's address.
        mvc.perform(authed(get("/api/donations"), GN, rgToken)).andExpect(status().isUnauthorized());

        // The Gurdwara's own admin sees none of Radha Govinda's donations.
        String gnToken = login(GN);
        mvc.perform(authed(get("/api/donations"), GN, gnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());

        // And at the database level, RLS hides everything when no temple is in context.
        assertThat(jdbc.queryForObject("select count(*) from donation", Long.class)).isZero();
    }

    @Test
    void apiNeedsSignIn() throws Exception {
        mvc.perform(get("/api/contacts").with(host(RG))).andExpect(status().isUnauthorized());
    }

    private String login(String hostName) throws Exception {
        var res = mvc.perform(post("/api/auth/login").with(host(hostName)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.local\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse();
        String setCookie = res.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("SameSite=Strict");
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }

    private static MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder b, String hostName, String token) {
        return b.with(host(hostName)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }
}
