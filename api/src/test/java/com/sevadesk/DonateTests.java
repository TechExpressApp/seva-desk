package com.sevadesk;

import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.donors.Donation;
import com.sevadesk.donors.DonationRepository;
import com.sevadesk.donors.FundRepository;
import com.sevadesk.payments.GiftCheckout;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The /donate page and Stripe webhooks turning paid gifts into SevaDesk donations. No network calls. */
class DonateTests extends PostgresTestBase {

    static final String SITE = "www.iskconsomerset.org";
    static final String WEBHOOK_SECRET = "whsec_test_secret"; // from application-test.yml

    @Autowired MockMvc mvc;
    @Autowired TenantRepository tenants;
    @Autowired DonationRepository donations;
    @Autowired ContactRepository contacts;
    @Autowired FundRepository funds;

    @Test
    void donatePageShowsGivingOptionsAndCampaigns() throws Exception {
        mvc.perform(get("/donate").with(host(SITE)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Sunday Feast Prasadam Sponsor")))
                .andExpect(content().string(containsString("data-amount=\"151\"")))
                .andExpect(content().string(containsString("value=\"MONTHLY\"")))
                .andExpect(content().string(containsString("1000 Fortunate Souls")))
                // Nitya Sevak: four patron levels, or any amount
                .andExpect(content().string(containsString("Platinum Patron")))
                .andExpect(content().string(containsString("Diamond Patron")))
                .andExpect(content().string(containsString("Gold Patron")))
                .andExpect(content().string(containsString("Silver Patron")))
                .andExpect(content().string(containsString("$216")))
                .andExpect(content().string(containsString("$54")))
                .andExpect(content().string(containsString("Any amount you like")))
                .andExpect(content().string(containsString("Zelle")))
                .andExpect(content().string(containsString("Test mode")));
    }

    @Test
    void invalidAmountsGoBackToTheFormWithAMessage() throws Exception {
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("frequency", "ONE_TIME")
                        .param("option", "other").param("otherAmount", "abc"))
                .andExpect(redirectedUrl("/donate?error=Please+enter+an+amount+between+%241+and+%2450%2C000.#give"));
        mvc.perform(post("/donate/checkout").with(host(SITE)).param("frequency", "DAILY").param("option", "0"))
                .andExpect(redirectedUrl("/donate?error=Please+choose+how+often+to+give.#give"));
    }

    @Test
    void feeCoverMakesTheTempleWhole() {
        // $108 gift + fee so that after Stripe's 2.9% + 30¢ the temple receives $108
        BigDecimal fee = GiftCheckout.feeFor(new BigDecimal("108"));
        BigDecimal charged = new BigDecimal("108").add(fee);
        BigDecimal stripeFee = charged.multiply(new BigDecimal("0.029")).add(new BigDecimal("0.30"));
        assertThat(charged.subtract(stripeFee)).isBetween(new BigDecimal("107.99"), new BigDecimal("108.01"));
    }

    @Test
    void webhookWithABadSignatureIsRejected() throws Exception {
        mvc.perform(post("/api/public/stripe/webhook").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=deadbeef").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void paidCheckoutBecomesADonationWithAReceiptOnlyOnce() throws Exception {
        String pi = "pi_test_" + UUID.randomUUID().toString().replace("-", "");
        String email = "donor-" + UUID.randomUUID() + "@example.com";
        String payload = """
            {"id":"evt_1","type":"checkout.session.completed","created":%d,
             "data":{"object":{"id":"cs_test_1","object":"checkout.session","mode":"payment","payment_status":"paid",
               "amount_total":15553,"currency":"usd","payment_intent":"%s",
               "customer_details":{"email":"%s","name":"Gopal Krishna Das"},
               "metadata":{"tenant":"iskconsomerset","fund":"Sunday Feast","label":"Sunday Feast Prasadam Sponsor – ISKCON Somerset",
                 "occasion":"Mother's birthday","fee_covered":"4.53","campaign":"","installments":"","frequency":"ONE_TIME"}}}}
            """.formatted(Instant.now().getEpochSecond(), pi, email);

        send(payload);
        send(payload); // Stripe retries are harmless

        var recorded = asTemple(() -> donations.findAll().stream()
                .filter(d -> pi.equals(d.getReference())).toList());
        assertThat(recorded).hasSize(1);
        Donation d = recorded.get(0);
        assertThat(d.getAmount()).isEqualByComparingTo("155.53");
        assertThat(d.getPaymentMode()).isEqualTo(Donation.PaymentMode.ONLINE);
        assertThat(d.getReceiptNo()).matches("ISK/\\d{4}/\\d{5}");
        assertThat(d.getNotes()).contains("in honor of: Mother's birthday").contains("donor covered $4.53 fees");
        assertThat(asTemple(() -> funds.findById(d.getFundId()).orElseThrow().getName())).isEqualTo("Sunday Feast");
        var donor = asTemple(() -> contacts.findById(d.getContactId()).orElseThrow());
        assertThat(donor.getEmail()).isEqualTo(email);
        assertThat(donor.getFirstName()).isEqualTo("Gopal");
    }

    @Test
    void simultaneousDuplicateDeliveriesRecordOnceAndBothSucceed() throws Exception {
        String invoice = "in_dup_" + UUID.randomUUID().toString().replace("-", "");
        String payload = """
            {"id":"evt_dup","type":"invoice.paid","created":%d,
             "data":{"object":{"id":"%s","object":"invoice","amount_paid":15100,"customer_email":"twice@example.com",
               "parent":{"subscription_details":{"metadata":{"fund":"Sunday Feast","frequency":"MONTHLY"}}}}}}
            """.formatted(Instant.now().getEpochSecond(), invoice);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var results = new java.util.ArrayList<java.util.concurrent.Future<Integer>>();
            for (int i = 0; i < 4; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(post("/api/public/stripe/webhook").with(host(SITE))
                            .contentType(MediaType.APPLICATION_JSON).header("Stripe-Signature", sign(payload))
                            .content(payload)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (var r : results) assertThat(r.get()).isEqualTo(200);
        } finally {
            pool.shutdown();
        }
        assertThat(asTemple(() -> donations.findAll().stream().filter(x -> invoice.equals(x.getReference())).count()))
                .isEqualTo(1);
    }

    @Test
    void eachPaidMonthlyInvoiceIsRecorded() throws Exception {
        String invoice = "in_test_" + UUID.randomUUID().toString().replace("-", "");
        send("""
            {"id":"evt_2","type":"invoice.paid","created":%d,
             "data":{"object":{"id":"%s","object":"invoice","amount_paid":5100,"customer_email":"sevak@example.com",
               "customer_name":"Nitya Sevak","status_transitions":{"paid_at":%d},
               "parent":{"type":"subscription_details","subscription_details":{"subscription":"sub_123",
                 "metadata":{"tenant":"iskconsomerset","fund":"Nitya Sevak","label":"Nitya Sevak – ISKCON Somerset",
                   "frequency":"MONTHLY","campaign":"nitya-sevak","installments":"","occasion":"","fee_covered":"0"}}}}}}
            """.formatted(Instant.now().getEpochSecond(), invoice, Instant.now().getEpochSecond()));

        var d = asTemple(() -> donations.findAll().stream().filter(x -> invoice.equals(x.getReference())).findFirst().orElseThrow());
        assertThat(d.getAmount()).isEqualByComparingTo("51.00");
        assertThat(d.getNotes()).contains("Monthly gift").contains("campaign: nitya-sevak");
    }

    @Test
    void invoicesFromOtherStripeProductsAreIgnored() throws Exception {
        String invoice = "in_other_" + UUID.randomUUID().toString().replace("-", "");
        send("""
            {"id":"evt_3","type":"invoice.paid","created":%d,
             "data":{"object":{"id":"%s","object":"invoice","amount_paid":999,"parent":{"subscription_details":{"metadata":{}}}}}}
            """.formatted(Instant.now().getEpochSecond(), invoice));
        assertThat(asTemple(() -> donations.findAll().stream().anyMatch(x -> invoice.equals(x.getReference())))).isFalse();
    }

    private void send(String payload) throws Exception {
        mvc.perform(post("/api/public/stripe/webhook").with(host(SITE)).contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", sign(payload)).content(payload))
                .andExpect(status().isOk());
    }

    /** Same scheme Stripe uses: t=timestamp,v1=HMAC-SHA256(secret, "timestamp.payload") */
    private static String sign(String payload) throws Exception {
        long t = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig = HexFormat.of().formatHex(mac.doFinal((t + "." + payload).getBytes(StandardCharsets.UTF_8)));
        return "t=" + t + ",v1=" + sig;
    }

    private <T> T asTemple(Supplier<T> work) {
        var t = tenants.findBySlugAndActiveTrue("iskconsomerset").orElseThrow();
        return TenantContext.runAs(t.toRef(), work);
    }
}
