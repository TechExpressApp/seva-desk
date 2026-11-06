package com.sevadesk;

import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.events.EventRegistration;
import com.sevadesk.events.EventRegistrationRepository;
import com.sevadesk.platform.Mailer;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public /register page that "Join us" on the home page opens. */
class RegisterPageTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";

    @Autowired MockMvc mvc;
    @Autowired TenantRepository tenants;
    @Autowired EventRegistrationRepository registrations;
    @Autowired ContactRepository contacts;
    @MockitoBean Mailer mailer;

    @BeforeEach
    void setUp() {
        clearInvocations(mailer);
        when(mailer.send(anyString(), anyString(), anyString())).thenReturn(true);
    }

    @Test
    void joinUsOnTheHomePageOpensRegistrationForThatGathering() throws Exception {
        mvc.perform(get("/").with(host(SITE)))
                .andExpect(content().string(containsString("href=\"/register?event=Sunday%20Love%20Feast\"")))
                .andExpect(content().string(containsString("/register?event=Bhakti%20Vriksha%20%26%20Families")));

        mvc.perform(get("/register").param("event", "sunday love feast").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>Sunday Love Feast</h1>")))
                .andExpect(content().string(containsString("<option value=\"Sunday Love Feast\" selected=\"selected\">")))
                .andExpect(content().string(containsString("name=\"whatsapp\"")));
    }

    @Test
    void theBhaktiVrikshaPageEndsWithTheFormForItsGroups() throws Exception {
        mvc.perform(get("/bhakti-vriksha").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("the tree of devotion")))
                .andExpect(content().string(containsString("How Bhakti Vriksha helps in daily life")))
                .andExpect(content().string(containsString("Register for Bhakti Vriksha &amp; Families")))
                .andExpect(content().string(containsString("action=\"/register\"")))
                .andExpect(content().string(containsString(
                        "<option value=\"Bhakti Vriksha &amp; Families\" selected=\"selected\">")));
        // other pages have no form
        mvc.perform(get("/guru-vandana").with(host(SITE)))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("action=\"/register\""))));
    }

    @Test
    void aVisitorRegistersAndTheTempleIsEmailed() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String event = "Website Test Festival " + tag;
        String email = "visitor." + tag + "@example.com";

        mvc.perform(form(event, "Nanda", "Kishore " + tag, email, "+1 732 555 0123", "Bridgewater", "New Jersey", "45"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register/thank-you?event=Website+Test+Festival+" + tag + "&name=Nanda"));

        var saved = registrationsFor(event);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getEmail()).isEqualTo(email);
        assertThat(saved.get(0).getAge()).isEqualTo(45);
        assertThat(saved.get(0).getContactId()).isNotNull();
        verify(mailer).send(eq("iskcon.somerset@gmail.com"), contains(event), contains("Nanda Kishore " + tag));

        mvc.perform(get("/register/thank-you").param("event", event).param("name", "Nanda").with(host(SITE)))
                .andExpect(content().string(containsString("Thank you, Nanda!")))
                .andExpect(content().string(containsString(event)));
    }

    @Test
    void mistakesAreExplainedAndWhatWasTypedIsKept() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String event = "Website Test Festival " + tag;
        mvc.perform(form(event, "Radha", "Devi", "not-an-email", "+1 732 555 0123", "Edison", "NJ", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Please enter a valid email address.")))
                .andExpect(content().string(containsString("value=\"Radha\"")))
                .andExpect(content().string(containsString("value=\"Edison\"")));
        mvc.perform(form(event, "Radha", "Devi", "radha@example.com", "+1 732 555 0123", "Edison", "NJ", "abc"))
                .andExpect(content().string(containsString("Please enter your age.")));
        mvc.perform(form(event, "Radha", "Devi", "radha@example.com", "call me", "Edison", "NJ", "30"))
                .andExpect(content().string(containsString("WhatsApp number")));
        assertThat(registrationsFor(event)).isEmpty();
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void botsFillingTheSpamTrapAreIgnored() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String event = "Website Test Festival " + tag;
        mvc.perform(form(event, "Spam", "Bot", "bot@example.com", "+1 732 555 0123", "X", "Y", "30").param("website", "http://spam"))
                .andExpect(redirectedUrl("/register/thank-you"));
        assertThat(registrationsFor(event)).isEmpty();
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    private MockHttpServletRequestBuilder form(String event, String first, String last, String email, String whatsapp,
                                               String city, String state, String age) {
        return post("/register").with(host(SITE))
                .param("eventName", event).param("firstName", first).param("lastName", last).param("email", email)
                .param("whatsapp", whatsapp).param("city", city).param("state", state).param("age", age);
    }

    private List<EventRegistration> registrationsFor(String event) {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        var out = new AtomicReference<List<EventRegistration>>();
        TenantContext.runAs(t.toRef(), () -> out.set(registrations.search(event, null, PageRequest.of(0, 10)).getContent()));
        return out.get();
    }
}
