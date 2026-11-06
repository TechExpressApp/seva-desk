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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Expenses: paid to a contact, on a date, for one of the fixed purposes. */
class ExpenseTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired TenantRepository tenants;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    String treasurer;
    String frontDesk;
    String vendorId;
    LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));

    @BeforeEach
    void setUp() throws Exception {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        TenantContext.runAs(t.toRef(), () -> {
            for (var u : List.of(new String[]{"exp-treasurer@test.local", "TREASURER"}, new String[]{"exp-desk@test.local", "FRONT_DESK"})) {
                if (users.findByEmailIgnoreCase(u[0]).isEmpty()) {
                    users.save(new AppUser(u[0], encoder.encode("test-password-1"), "Tester", List.of(u[1])));
                }
            }
        });
        treasurer = login("exp-treasurer@test.local");
        frontDesk = login("exp-desk@test.local");
        vendorId = call(post("/api/contacts"), treasurer,
                "{\"firstName\":\"Govinda\",\"lastName\":\"Grocers " + UUID.randomUUID().toString().substring(0, 6) + "\"}", 201)
                .get("id").asText();
    }

    @Test
    void addEditListAndDeleteAnExpense() throws Exception {
        JsonNode created = call(post("/api/expenses"), treasurer, """
                {"contactId":"%s","spentOn":"%s","amount":1250.50,"purpose":"GROCERY","notes":"Sunday feast vegetables"}
                """.formatted(vendorId, today), 201);
        assertThat(created.get("name").asText()).startsWith("Govinda Grocers");
        assertThat(created.get("purposeLabel").asText()).isEqualTo("Grocery");
        String id = created.get("id").asText();

        JsonNode updated = call(put("/api/expenses/" + id), treasurer, """
                {"contactId":"%s","spentOn":"%s","amount":1300,"purpose":"DEITY_WORSHIP"}
                """.formatted(vendorId, today), 200);
        assertThat(updated.get("purposeLabel").asText()).isEqualTo("Deity Worship");
        assertThat(updated.get("notes").isNull()).isTrue();

        // filters: by purpose and by person
        JsonNode byPurpose = call(get("/api/expenses").param("purpose", "DEITY_WORSHIP").param("contactId", vendorId), treasurer, null, 200);
        assertThat(byPurpose.at("/page/totalElements").asLong()).isEqualTo(1);
        JsonNode otherPurpose = call(get("/api/expenses").param("purpose", "BOOKS").param("contactId", vendorId), treasurer, null, 200);
        assertThat(otherPurpose.at("/page/totalElements").asLong()).isZero();
        JsonNode inRange = call(get("/api/expenses").param("contactId", vendorId)
                .param("from", today.minusDays(1).toString()).param("to", today.toString()), treasurer, null, 200);
        assertThat(inRange.at("/page/totalElements").asLong()).isEqualTo(1);

        JsonNode summary = call(get("/api/expenses/summary"), treasurer, null, 200);
        assertThat(summary.get("thisMonth").decimalValue()).isGreaterThanOrEqualTo(new java.math.BigDecimal("1300"));
        assertThat(summary.get("thisMonthByPurpose").toString()).contains("Deity Worship");

        call(delete("/api/expenses/" + id), treasurer, null, 204);
        assertThat(call(get("/api/expenses").param("contactId", vendorId), treasurer, null, 200)
                .at("/page/totalElements").asLong()).isZero();
    }

    @Test
    void expensesAddUpPerPerson() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 6);
        String big = call(post("/api/contacts"), treasurer, "{\"firstName\":\"Anant\",\"lastName\":\"Builders " + tag + "\"}", 201).get("id").asText();
        String small = call(post("/api/contacts"), treasurer, "{\"firstName\":\"Bhima\",\"lastName\":\"Books " + tag + "\"}", 201).get("id").asText();
        for (String body : List.of(
                "{\"contactId\":\"%s\",\"spentOn\":\"%s\",\"amount\":5000,\"purpose\":\"CONSTRUCTION\"}".formatted(big, today),
                "{\"contactId\":\"%s\",\"spentOn\":\"%s\",\"amount\":2500,\"purpose\":\"CONSTRUCTION\"}".formatted(big, today.minusDays(3)),
                "{\"contactId\":\"%s\",\"spentOn\":\"%s\",\"amount\":120.50,\"purpose\":\"BOOKS\"}".formatted(small, today))) {
            call(post("/api/expenses"), treasurer, body, 201);
        }

        JsonNode page = call(get("/api/expenses/by-person").param("q", tag), treasurer, null, 200);
        assertThat(page.get("totalPeople").asLong()).isEqualTo(2);
        assertThat(page.get("grandTotal").decimalValue()).isEqualByComparingTo("7620.50");
        assertThat(page.at("/content/0/name").asText()).startsWith("Anant");
        assertThat(page.at("/content/0/expenses").asLong()).isEqualTo(2);
        assertThat(page.at("/content/0/total").decimalValue()).isEqualByComparingTo("7500");
        assertThat(page.at("/content/0/lastExpense").asText()).isEqualTo(today.toString());

        JsonNode byName = call(get("/api/expenses/by-person").param("q", tag).param("sort", "name").param("dir", "desc"), treasurer, null, 200);
        assertThat(byName.at("/content/0/name").asText()).startsWith("Bhima");
        JsonNode byCount = call(get("/api/expenses/by-person").param("q", tag).param("sort", "expenses").param("dir", "asc"), treasurer, null, 200);
        assertThat(byCount.at("/content/0/name").asText()).startsWith("Bhima");

        assertThat(call(get("/api/expenses/by-person/" + small), treasurer, null, 200).get("total").decimalValue())
                .isEqualByComparingTo("120.50");
        call(get("/api/expenses/by-person"), frontDesk, null, 403);
    }

    @Test
    void theSevenPurposesAreOffered() throws Exception {
        JsonNode purposes = call(get("/api/expenses/purposes"), treasurer, null, 200);
        List<String> labels = new java.util.ArrayList<>();
        purposes.forEach(p -> labels.add(p.get("label").asText()));
        assertThat(labels).containsExactly("Building Maintenance", "Construction", "Deity Worship", "Grocery",
                "Monk Donation", "Books", "Other");
    }

    @Test
    void nameMustBeAContactAndPurposeFromTheList() throws Exception {
        call(post("/api/expenses"), treasurer, """
                {"spentOn":"%s","amount":10,"purpose":"BOOKS"}""".formatted(today), 400);
        call(post("/api/expenses"), treasurer, """
                {"contactId":"%s","spentOn":"%s","amount":10,"purpose":"BOOKS"}""".formatted(UUID.randomUUID(), today), 400);
        call(post("/api/expenses"), treasurer, """
                {"contactId":"%s","spentOn":"%s","amount":10,"purpose":"PARTY"}""".formatted(vendorId, today), 400);
        call(post("/api/expenses"), treasurer, """
                {"contactId":"%s","spentOn":"%s","amount":-5,"purpose":"BOOKS"}""".formatted(vendorId, today), 400);
    }

    @Test
    void onlyTreasurersAndAdminsSeeExpenses() throws Exception {
        call(get("/api/expenses"), frontDesk, null, 403);
        call(post("/api/expenses"), frontDesk, """
                {"contactId":"%s","spentOn":"%s","amount":10,"purpose":"BOOKS"}""".formatted(vendorId, today), 403);
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
