package com.sevadesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.platform.AppUser;
import com.sevadesk.platform.AppUserRepository;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public website for www.iskconsomerset.org, and events flowing from the admin API onto it. */
class WebsiteTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void admin() {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> {
            if (users.findByEmailIgnoreCase("web-admin@test.local").isEmpty()) {
                users.save(new AppUser("web-admin@test.local", encoder.encode("test-password"), "Admin", List.of("ADMIN")));
            }
        });
    }

    @Test
    void homePageHasTheTemplesMenuAndAnAdminLink() throws Exception {
        mvc.perform(get("/").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("ISKCON Somerset")))
                .andExpect(content().string(containsString("href=\"/bhakti-vriksha\"")))
                .andExpect(content().string(containsString("Vaishnava Songs")))
                .andExpect(content().string(containsString("href=\"/admin/\"")))
                .andExpect(content().string(containsString("\"@type\":\"HinduTemple\"")));
        // the bare domain is the same temple
        mvc.perform(get("/").with(host("iskconsomerset.org"))).andExpect(status().isOk());
    }

    @Test
    void publishedEventsAppearOnTheWebsiteImmediatelyAndDraftsDoNot() throws Exception {
        String token = login();
        Instant start = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

        String published = json.readTree(mvc.perform(post("/api/events").with(host(SITE))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"Govardhan Puja & Annakut","summary":"108 preparations</script>","startsAt":"%s",
                             "location":"Main hall","status":"PUBLISHED"}""".formatted(start)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("slug").asText();

        String draft = json.readTree(mvc.perform(post("/api/events").with(host(SITE))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"title":"Secret Planning Meeting","startsAt":"%s","status":"DRAFT"}""".formatted(start)))
                .andReturn().getResponse().getContentAsString()).get("slug").asText();

        assertThat(published).startsWith("govardhan-puja-annakut-");

        mvc.perform(get("/").with(host(SITE)))
                .andExpect(content().string(containsString("Govardhan Puja &amp; Annakut")))
                .andExpect(content().string(not(containsString("Secret Planning Meeting"))));

        mvc.perform(get("/events/" + published).with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"@type\":\"Event\"")))
                // text from staff can't break out of the structured-data script block
                .andExpect(content().string(containsString("108 preparations<\\/script>")));

        mvc.perform(get("/events/" + draft).with(host(SITE))).andExpect(status().isNotFound());

        mvc.perform(get("/sitemap.xml").with(host(SITE)))
                .andExpect(content().string(containsString("/events/" + published)))
                .andExpect(content().string(containsString("/bhakti-vriksha")))
                .andExpect(content().string(not(containsString(draft))));
    }

    @Test
    void oldSiteUrlsStillWork() throws Exception {
        mvc.perform(get("/bhakti-vriksha").with(host(SITE))).andExpect(status().isOk())
                .andExpect(content().string(containsString("<title>Bhakti Vriksha | ISKCON Somerset</title>")));
        mvc.perform(get("/category/all-products").with(host(SITE))).andExpect(status().isOk());
        mvc.perform(get("/guru-vandana").with(host(SITE))).andExpect(status().isOk());
        mvc.perform(get("/no-such-page").with(host(SITE))).andExpect(status().isNotFound());
    }

    @Test
    void subscribeFormCreatesAContactWithEmailConsent() throws Exception {
        mvc.perform(post("/subscribe").with(host(SITE))
                        .param("firstName", "Radhika").param("lastName", "Sharma")
                        .param("email", "Radhika.Sharma@example.com").param("whatsapp", "+1 732 555 0101")
                        .param("initiatedName", "Radhika devi dasi")
                        .param("spouseName", "Madhav Sharma").param("spouseBirthDate", "1984-03-21")
                        .param("child1Name", "Gopal").param("child1BirthDate", "2014-08-26")
                        .param("child2Name", "").param("child2BirthDate", "")
                        .param("child3Name", "Tulasi").param("child3BirthDate", "2019-11-02"))
                .andExpect(redirectedUrl("/contact-us?subscribed"));

        mvc.perform(get("/api/contacts").param("q", "radhika.sharma@example.com").with(host(SITE))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + login()))
                .andExpect(content().string(containsString("\"consentEmail\":true")))
                .andExpect(content().string(containsString("Radhika devi dasi")))
                .andExpect(content().string(containsString("\"spouse\":{\"name\":\"Madhav Sharma\",\"birthDate\":\"1984-03-21\"}")))
                // the empty Child 2 row is dropped; Child 3 becomes the second child
                .andExpect(content().string(containsString("\"children\":[{\"name\":\"Gopal\",\"birthDate\":\"2014-08-26\"},{\"name\":\"Tulasi\",\"birthDate\":\"2019-11-02\"}]")))
                .andExpect(content().string(not(containsString("\"empty\""))));

        // bots that fill the hidden field are silently ignored
        mvc.perform(post("/subscribe").with(host(SITE)).param("firstName", "Bot").param("email", "bot@example.com")
                .param("website", "http://spam.example")).andExpect(redirectedUrl("/contact-us?subscribed"));
    }

    @Test
    void adminSavesFamilyBirthdaysAndKeepsDetailsFromTheWebsite() throws Exception {
        String token = login();
        String email = "family-" + java.util.UUID.randomUUID() + "@example.com";
        mvc.perform(post("/subscribe").with(host(SITE)).param("firstName", "Keshav").param("email", email)
                .param("initiatedName", "Keshava das")).andExpect(redirectedUrl("/contact-us?subscribed"));

        String id = json.readTree(mvc.perform(get("/api/contacts").param("q", email).with(host(SITE))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("content").get(0).get("id").asText();

        // the admin form sends no customFields: the initiated name must survive the update
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/contacts/" + id)
                        .with(host(SITE)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"firstName":"Keshav","email":"%s","family":{
                              "spouse":{"name":"Radha","birthDate":"1990-01-15"},
                              "children":[{"name":"Hari","birthDate":"2018-06-01"},{"name":"","birthDate":null}]}}
                            """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Keshava das")))
                .andExpect(content().string(containsString("\"spouse\":{\"name\":\"Radha\",\"birthDate\":\"1990-01-15\"}")))
                .andExpect(content().string(containsString("\"children\":[{\"name\":\"Hari\",\"birthDate\":\"2018-06-01\"}]")));

        // a fourth child is refused
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/contacts/" + id)
                        .with(host(SITE)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                            {"firstName":"Keshav","family":{"children":[{"name":"A"},{"name":"B"},{"name":"C"},{"name":"D"}]}}
                            """))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("\"name\":\"D\""))));
    }

    @Test
    void publicPagesIgnoreStaleSignInCookies() throws Exception {
        mvc.perform(get("/").with(host(SITE)).cookie(new Cookie("SD_TOKEN", "expired-or-garbage")))
                .andExpect(status().isOk());
        // the admin app's startup call and the login itself must work with a stale cookie too
        mvc.perform(get("/api/public/branding").with(host(SITE)).cookie(new Cookie("SD_TOKEN", "expired-or-garbage")))
                .andExpect(status().isOk());
        mvc.perform(post("/api/auth/login").with(host(SITE)).cookie(new Cookie("SD_TOKEN", "expired-or-garbage"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"web-admin@test.local\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEnginesAreKeptOutOfTheAdmin() throws Exception {
        mvc.perform(get("/robots.txt").with(host(SITE)))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Sitemap: http://www.iskconsomerset.org/sitemap.xml")));
    }

    private String login() throws Exception {
        String setCookie = mvc.perform(post("/api/auth/login").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"web-admin@test.local\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
    }
}
