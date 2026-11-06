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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Donations find their donor by email, phone or full name; otherwise a new contact is created.
 * Each person's donations then add up to one total.
 */
class DonorMatchingTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    String token;
    String fundId;
    String tag; // keeps each test's people unique in the shared database

    @BeforeEach
    void setUp() throws Exception {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> {
            if (users.findByEmailIgnoreCase("match-admin@test.local").isEmpty()) {
                users.save(new AppUser("match-admin@test.local", encoder.encode("test-password"), "Admin", List.of("ADMIN")));
            }
        });
        String setCookie = mvc.perform(post("/api/auth/login").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"match-admin@test.local\",\"password\":\"test-password\"}"))
                .andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);
        token = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        fundId = call(post("/api/funds"), "{\"name\":\"Match Fund " + UUID.randomUUID() + "\",\"taxDeductible\":true}")
                .get("id").asText();
        tag = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void sameEmailDifferentCaseIsTheSamePerson() throws Exception {
        JsonNode first = donate(100, "Anand", "Kumar" + tag, "anand." + tag + "@Example.com", null);
        JsonNode second = donate(50, "A.", "K.", "ANAND." + tag + "@example.COM", null);
        assertThat(first.at("/donorLink/matchedBy").asText()).isEqualTo("CREATED");
        assertThat(second.at("/donorLink/matchedBy").asText()).isEqualTo("EMAIL");
        assertThat(second.get("contactId").asText()).isEqualTo(first.get("contactId").asText());
        assertThat(total(first.get("contactId").asText())).isEqualByComparingTo("150");
    }

    @Test
    void samePhoneInAnyFormatIsTheSamePerson() throws Exception {
        String digits = String.valueOf(Math.abs(tag.hashCode()) % 9_000_000 + 1_000_000); // 7 digits
        JsonNode first = donate(51, "Bhakti", "Lata" + tag, null, "+1 (732) " + digits.substring(0, 3) + "-" + digits.substring(3));
        JsonNode second = donate(21, "Someone", "Else", null, "732" + digits);
        assertThat(second.at("/donorLink/matchedBy").asText()).isEqualTo("PHONE");
        assertThat(second.get("contactId").asText()).isEqualTo(first.get("contactId").asText());
    }

    @Test
    void sameFullNameIgnoringCaseAndSplitIsTheSamePerson() throws Exception {
        JsonNode first = donate(108, "Gopal Krishna", "Das" + tag, null, null);
        JsonNode second = donate(11, "gopal", "  KRISHNA   das" + tag, null, null);
        assertThat(second.at("/donorLink/matchedBy").asText()).isEqualTo("NAME");
        assertThat(second.get("contactId").asText()).isEqualTo(first.get("contactId").asText());
        assertThat(second.at("/donorName").asText()).isEqualTo("Gopal Krishna Das" + tag);
    }

    @Test
    void noMatchCreatesANewContactAndMatchesFillInMissingDetails() throws Exception {
        JsonNode first = donate(25, "Chaitanya", "Rao" + tag, null, null);
        assertThat(first.at("/donorLink/matchedBy").asText()).isEqualTo("CREATED");
        // matched by name later, with an email: the email is added to the contact
        donate(25, "Chaitanya", "Rao" + tag, "chaitanya." + tag + "@example.com", null);
        JsonNode contact = call(get("/api/contacts/" + first.get("contactId").asText()), null);
        assertThat(contact.get("email").asText()).isEqualTo("chaitanya." + tag + "@example.com");
        // and now a gift with only that email finds the same person
        JsonNode third = donate(25, null, null, "chaitanya." + tag + "@example.com", null);
        assertThat(third.get("contactId").asText()).isEqualTo(first.get("contactId").asText());
        assertThat(total(first.get("contactId").asText())).isEqualByComparingTo("75");
    }

    @Test
    void noDetailsMeansAnonymous() throws Exception {
        JsonNode d = donate(10, null, null, null, null);
        assertThat(d.get("contactId").isNull()).isTrue();
        assertThat(d.get("donorName").asText()).isEqualTo("Anonymous");
    }

    @Test
    void donorTotalsExcludeVoidedGiftsAndListBiggestGiversFirst() throws Exception {
        JsonNode big = donate(1000, "Big", "Giver" + tag, null, null);
        donate(500, "Big", "Giver" + tag, null, null);
        JsonNode mistake = donate(9999, "Big", "Giver" + tag, null, null);
        call(post("/api/donations/" + mistake.get("id").asText() + "/void"), "{\"reason\":\"Entered twice\"}");
        donate(10, "Small", "Giver" + tag, null, null);

        JsonNode page = call(get("/api/donors").param("q", "giver" + tag), null);
        assertThat(page.get("totalDonors").asLong()).isEqualTo(2);
        assertThat(page.at("/content/0/name").asText()).isEqualTo("Big Giver" + tag);
        assertThat(page.at("/content/0/total").decimalValue()).isEqualByComparingTo("1500");
        assertThat(page.at("/content/0/gifts").asLong()).isEqualTo(2);
        assertThat(page.at("/content/1/total").decimalValue()).isEqualByComparingTo("10");
        assertThat(page.get("grandTotal").decimalValue()).isEqualByComparingTo("1510");
        assertThat(total(big.get("contactId").asText())).isEqualByComparingTo("1500");
    }

    @Test
    void donorsCanBeSortedByAnyColumnInEitherDirection() throws Exception {
        // Ann: 1 gift of 300 · Bala: 2 gifts of 50 · Chandra: 3 gifts of 10 (no email)
        donate(300, "Ann", "Sort" + tag, "ann." + tag + "@example.com", null);
        donate(50, "Bala", "Sort" + tag, "bala." + tag + "@example.com", null);
        donate(50, "Bala", "Sort" + tag, null, null);
        for (int i = 0; i < 3; i++) donate(10, "Chandra", "Sort" + tag, null, null);

        assertThat(names("name", "asc")).containsExactly("Ann", "Bala", "Chandra");
        assertThat(names("name", "desc")).containsExactly("Chandra", "Bala", "Ann");
        assertThat(names("total", "desc")).containsExactly("Ann", "Bala", "Chandra");
        assertThat(names("total", "asc")).containsExactly("Chandra", "Bala", "Ann");
        assertThat(names("gifts", "desc")).containsExactly("Chandra", "Bala", "Ann");
        assertThat(names("gifts", "asc")).containsExactly("Ann", "Bala", "Chandra");
        assertThat(names("lastGift", "desc").get(0)).isEqualTo("Chandra"); // most recent gift
        // no email sorts last in both directions
        assertThat(names("email", "asc")).containsExactly("Ann", "Bala", "Chandra");
        assertThat(names("email", "desc")).containsExactly("Bala", "Ann", "Chandra");
        // anything unexpected falls back to total, never into the SQL
        assertThat(names("total; drop table contact", "sideways")).containsExactly("Ann", "Bala", "Chandra");
    }

    private List<String> names(String sort, String dir) throws Exception {
        JsonNode page = call(get("/api/donors").param("q", "sort" + tag).param("sort", sort).param("dir", dir), null);
        List<String> out = new java.util.ArrayList<>();
        page.get("content").forEach(n -> out.add(n.get("name").asText().split(" ")[0]));
        return out;
    }

    private JsonNode donate(int amount, String first, String last, String email, String phone) throws Exception {
        String body = json.writeValueAsString(new java.util.LinkedHashMap<String, Object>() {{
            put("fundId", fundId);
            put("amount", amount);
            put("paymentMode", "CASH");
            put("donorFirstName", first);
            put("donorLastName", last);
            put("donorEmail", email);
            put("donorPhone", phone);
        }});
        return call(post("/api/donations"), body);
    }

    private java.math.BigDecimal total(String contactId) throws Exception {
        return call(get("/api/donors/" + contactId), null).get("total").decimalValue();
    }

    private JsonNode call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req, String body)
            throws Exception {
        req.with(host(SITE)).header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(body);
        var res = mvc.perform(req).andExpect(status().is2xxSuccessful()).andReturn().getResponse();
        return json.readTree(res.getContentAsString());
    }
}
