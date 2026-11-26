package com.sevadesk;

import com.sevadesk.contacts.Contact;
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
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The Bhagavad-gita course page: its sign-up form saves a contact and sends the person to their language's group. */
class CourseSignupTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";
    static final String COURSE = "Bhagavad-gita 18-Day Certificate Course";
    static final String TELUGU_GROUP = "https://chat.whatsapp.com/Cd4enOezjRRLXBoTzTGl04?mode=gi_t";

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
    void theCoursePageShowsTheDetailsTheFormAndIsInTheMenu() throws Exception {
        mvc.perform(get("/bhagavad-gita-course").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Discover the Wisdom of the Bhagavad-gita")))
                .andExpect(content().string(containsString("Monday, November 30th")))
                .andExpect(content().string(containsString("Read the Gita. Live the Gita. Share the Gita.")))
                .andExpect(content().string(containsString("action=\"/course-signup#signup\"")))
                .andExpect(content().string(containsString("name=\"addressLine1\"")))
                .andExpect(content().string(containsString("name=\"zip\"")))
                .andExpect(content().string(containsString("<option value=\"Kannada\">Kannada</option>")))
                // the WhatsApp links are only given out after registering
                .andExpect(content().string(not(containsString("chat.whatsapp.com"))));
        mvc.perform(get("/").with(host(SITE)))
                .andExpect(content().string(containsString("href=\"/bhagavad-gita-course\"")));
    }

    @Test
    void signingUpSavesTheContactAndOpensTheGroupForTheChosenLanguage() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = "gita-" + tag + "@example.com";
        mvc.perform(form("Madhava", "Rao " + tag, "34", email, "+1 732 555 0142", "telugu"))
                .andExpect(redirectedUrl("/course-signup/joined?page=bhagavad-gita-course&language=Telugu&name=Madhava"));

        mvc.perform(get("/course-signup/joined").param("page", "bhagavad-gita-course").param("language", "Telugu")
                        .param("name", "Madhava").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Thank you, Madhava!")))
                .andExpect(content().string(containsString("href=\"" + TELUGU_GROUP.replace("&", "&amp;") + "\"")));

        Contact c = contactByEmail(email);
        assertThat(c.getCity()).isEqualTo("Edison");
        assertThat(c.getCustomFields())
                .containsEntry("addressLine1", "12 Lotus Lane")
                .containsEntry("addressLine2", "Apt 3")
                .containsEntry("state", "New Jersey")
                .containsEntry("zip", "08817")
                .containsEntry("preferredLanguage", "Telugu")
                .containsEntry("age", 34);
        assertThat(c.isConsentWhatsapp()).isTrue();

        var saved = registrationsFor(c);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getEventName()).isEqualTo(COURSE);
        assertThat(saved.get(0).getContactId()).isEqualTo(c.getId());
    }

    @Test
    void mistakesAreExplainedAndNothingIsSaved() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String email = "gita-" + tag + "@example.com";
        mvc.perform(form("Radha", "Devi", "30", email, "+1 732 555 0123", "Klingon"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Please choose your preferred language.")))
                .andExpect(content().string(containsString("value=\"Radha\"")))
                .andExpect(content().string(containsString("value=\"12 Lotus Lane\"")));
        mvc.perform(form("Radha", "Devi", "30", email, "+1 732 555 0123", "Hindi").param("zip", "x"))
                .andExpect(content().string(containsString("Please enter a valid ZIP code.")));
        mvc.perform(form("Radha", "Devi", "thirty", email, "+1 732 555 0123", "Hindi"))
                .andExpect(content().string(containsString("Please enter your age.")));
        var none = new AtomicReference<Boolean>();
        TenantContext.runAs(somerset(), () ->
                none.set(contacts.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(email).isEmpty()));
        assertThat(none.get()).as("no contact saved").isTrue();
    }

    @Test
    void unknownLanguagesAndPagesGiveNoLinks() throws Exception {
        mvc.perform(get("/course-signup/joined").param("page", "bhagavad-gita-course").param("language", "Klingon")
                        .with(host(SITE)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/course-signup/joined").param("page", "bhakti-vriksha").param("language", "Telugu")
                        .with(host(SITE)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/course-signup").with(host(SITE)).param("page", "guru-vandana").param("firstName", "X"))
                .andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder form(String first, String last, String age, String email, String whatsapp,
                                               String language) {
        return post("/course-signup").with(host(SITE))
                .param("page", "bhagavad-gita-course")
                .param("firstName", first).param("lastName", last).param("age", age)
                .param("addressLine1", "12 Lotus Lane").param("addressLine2", "Apt 3")
                .param("city", "Edison").param("state", "New Jersey").param("zip", "08817")
                .param("whatsapp", whatsapp).param("email", email).param("language", language);
    }

    private Contact contactByEmail(String email) {
        var out = new AtomicReference<Contact>();
        TenantContext.runAs(somerset(), () ->
                out.set(contacts.findFirstByEmailIgnoreCaseOrderByCreatedAtAsc(email).orElseThrow()));
        return out.get();
    }

    private List<EventRegistration> registrationsFor(Contact c) {
        var out = new AtomicReference<List<EventRegistration>>();
        TenantContext.runAs(somerset(), () -> out.set(registrations.search(COURSE, c.getId(), PageRequest.of(0, 10)).getContent()));
        return out.get();
    }

    private TenantContext.TenantRef somerset() {
        return tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow().toRef();
    }
}
