package com.sevadesk;

import com.sevadesk.payments.GiftCheckout.Frequency;
import com.sevadesk.payments.GiftCheckout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.hamcrest.Matchers.containsString;

/** Nitya Sevak on /donate: a patron level, or any amount the donor likes, becomes a monthly gift. Stripe is stubbed. */
class NityaSevakCheckoutTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";
    static final String STRIPE_PAGE = "https://checkout.stripe.test/session";

    @Autowired MockMvc mvc;
    @MockitoBean GiftCheckout checkout;

    @BeforeEach
    void setUp() {
        clearInvocations(checkout);
        when(checkout.enabled()).thenReturn(true);
        when(checkout.start(any(), anyString(), anyString())).thenReturn(STRIPE_PAGE);
    }

    @Test
    void aPatronLevelIsChargedMonthlyWithItsName() throws Exception {
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "nitya-sevak").param("campaignLevel", "0"))
                .andExpect(redirectedUrl(STRIPE_PAGE));
        var gift = captured();
        assertThat(gift.amount()).isEqualByComparingTo("216");
        assertThat(gift.label()).isEqualTo("Nitya Sevak – Platinum Patron");
        assertThat(gift.frequency()).isEqualTo(Frequency.MONTHLY);
        assertThat(gift.fundName()).isEqualTo("Nitya Sevak");
        assertThat(gift.campaign()).isEqualTo("nitya-sevak");

        clearInvocations(checkout);
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "nitya-sevak").param("campaignLevel", "3"))
                .andExpect(redirectedUrl(STRIPE_PAGE));
        assertThat(captured().amount()).isEqualByComparingTo("54");
        assertThat(captured().label()).isEqualTo("Nitya Sevak – Silver Patron");
    }

    @Test
    void anyAmountTheDonorLikes() throws Exception {
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "nitya-sevak")
                        .param("campaignLevel", "other").param("campaignAmount", "75"))
                .andExpect(redirectedUrl(STRIPE_PAGE));
        assertThat(captured().amount()).isEqualByComparingTo(new BigDecimal("75"));
        assertThat(captured().label()).isEqualTo("Nitya Sevak");
    }

    @Test
    void badChoicesGoBackWithAMessage() throws Exception {
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "nitya-sevak").param("campaignLevel", "9"))
                .andExpect(redirectedUrl("/donate?error=Please+choose+a+donation+option.#give"));
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "nitya-sevak")
                        .param("campaignLevel", "other").param("campaignAmount", "abc"))
                .andExpect(redirectedUrl("/donate?error=Please+enter+an+amount+between+%241+and+%2450%2C000.#give"));
        verify(checkout, never()).start(any(), anyString(), anyString());
    }

    @Test
    void theNewTemplePageTakesGiftsOfBricksAndSquareFeet() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/new-temple-project").with(host(SITE)))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(content().string(containsString("Support our upcoming New Temple")))
                .andExpect(content().string(containsString("Śrīmad-Bhāgavatam 11.27.52")))
                .andExpect(content().string(containsString("of $500,000 goal")))
                .andExpect(content().string(containsString("One Square Foot")))
                .andExpect(content().string(containsString("$3,330")))
                .andExpect(content().string(containsString("value=\"YEARLY\"")))
                .andExpect(content().string(containsString("name=\"from\" value=\"/new-temple-project\"")));

        // ten square feet, yearly
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "new-temple").param("from", "/new-temple-project")
                        .param("frequency", "YEARLY").param("campaignLevel", "2"))
                .andExpect(redirectedUrl(STRIPE_PAGE));
        var gift = captured();
        assertThat(gift.amount()).isEqualByComparingTo("3330");
        assertThat(gift.frequency()).isEqualTo(Frequency.YEARLY);
        assertThat(gift.fundName()).isEqualTo("New Temple");
        assertThat(gift.label()).isEqualTo("Build the New Temple – Ten Square Feet");

        // mistakes go back to the New Temple page, at the appeal
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "new-temple").param("from", "/new-temple-project")
                        .param("frequency", "DAILY").param("campaignLevel", "0"))
                .andExpect(redirectedUrl("/new-temple-project?error=Please+choose+how+often+to+give.#new-temple"));
        // ...but never to another site
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("campaign", "new-temple").param("from", "//evil.example")
                        .param("frequency", "DAILY").param("campaignLevel", "0"))
                .andExpect(redirectedUrl("/donate?error=Please+choose+how+often+to+give.#give"));
    }

    private GiftCheckout.Gift captured() {
        var gift = ArgumentCaptor.forClass(GiftCheckout.Gift.class);
        verify(checkout).start(gift.capture(), anyString(), anyString());
        return gift.getValue();
    }
}
