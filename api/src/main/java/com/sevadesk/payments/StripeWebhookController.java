package com.sevadesk.payments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sevadesk.donors.OnlineGifts;
import com.sevadesk.platform.TenantContext;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Stripe calls this after a donor pays: https://www.iskconsomerset.org/api/public/stripe/webhook
 * In the Stripe dashboard, add this URL as a webhook endpoint with the events:
 * checkout.session.completed, checkout.session.async_payment_succeeded, invoice.paid.
 * <ul>
 *   <li>One-time gifts are recorded when the checkout completes.</li>
 *   <li>Weekly / monthly / yearly gifts are recorded on every paid invoice (including the first).</li>
 * </ul>
 * The payload is read as plain JSON so it works whichever Stripe API version the endpoint uses.
 */
@RestController
class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeAccounts accounts;
    private final OnlineGifts gifts;
    private final GiftCheckout checkout;
    private final ObjectMapper json;

    StripeWebhookController(StripeAccounts accounts, OnlineGifts gifts, GiftCheckout checkout, ObjectMapper json) {
        this.accounts = accounts;
        this.gifts = gifts;
        this.checkout = checkout;
        this.json = json;
    }

    @PostMapping("/api/public/stripe/webhook")
    ResponseEntity<String> webhook(@RequestBody String payload,
                                   @RequestHeader(name = "Stripe-Signature", required = false) String signature) throws Exception {
        var account = accounts.current();
        if (account.isEmpty()) return ResponseEntity.notFound().build();
        if (signature == null) return ResponseEntity.badRequest().body("missing signature");
        try {
            Webhook.constructEvent(payload, signature, account.get().webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Rejected Stripe webhook with a bad signature for {}", TenantContext.require().slug());
            return ResponseEntity.badRequest().body("bad signature");
        }

        JsonNode event = json.readTree(payload);
        String type = event.path("type").asText();
        JsonNode obj = event.path("data").path("object");
        Instant created = Instant.ofEpochSecond(event.path("created").asLong(Instant.now().getEpochSecond()));

        // Other errors propagate as 500 so Stripe retries; recording is idempotent.
        try {
            switch (type) {
                case "checkout.session.completed", "checkout.session.async_payment_succeeded" -> onCheckout(obj, created);
                case "invoice.paid" -> onInvoicePaid(obj, created);
                default -> log.debug("Ignoring Stripe event {}", type);
            }
        } catch (DataIntegrityViolationException e) {
            // The same event arrived twice at the same moment and the other copy recorded it first
            // (the unique index on the payment reference stopped this one). Nothing to retry.
            log.info("Stripe event {} was recorded by a simultaneous delivery; ignoring duplicate", event.path("id").asText());
        }
        return ResponseEntity.ok("ok");
    }

    private void onCheckout(JsonNode session, Instant created) {
        JsonNode meta = session.path("metadata");
        if ("subscription".equals(session.path("mode").asText())) {
            // Payments are recorded per invoice; here we only cap fixed-length pledges.
            String installments = meta.path("installments").asText("");
            String subscription = text(session.path("subscription"));
            if (!installments.isBlank() && subscription != null) {
                var frequency = GiftCheckout.Frequency.valueOf(meta.path("frequency").asText("MONTHLY"));
                checkout.limitInstallments(subscription, Integer.parseInt(installments), frequency,
                        session.path("created").asLong(created.getEpochSecond()));
            }
            return;
        }
        if (!"paid".equals(session.path("payment_status").asText())) {
            log.info("Checkout {} completed but not yet paid; waiting for async payment", session.path("id").asText());
            return;
        }
        String reference = text(session.path("payment_intent"));
        if (reference == null) reference = session.path("id").asText();
        JsonNode customer = session.path("customer_details");
        gifts.record(new OnlineGifts.PaidGift(reference, cents(session.path("amount_total")),
                meta.path("fund").asText(null), text(customer.path("email")), text(customer.path("name")),
                created, note(meta, "One-time gift")));
    }

    private void onInvoicePaid(JsonNode invoice, Instant created) {
        BigDecimal amount = cents(invoice.path("amount_paid"));
        if (amount.signum() <= 0) return;
        // API 2025+: invoice.parent.subscription_details.metadata; older: invoice.subscription_details.metadata
        JsonNode meta = invoice.path("parent").path("subscription_details").path("metadata");
        if (meta.isMissingNode() || meta.isEmpty()) meta = invoice.path("subscription_details").path("metadata");
        if (meta.isMissingNode() || meta.isEmpty() || !meta.hasNonNull("fund")) {
            log.info("Ignoring invoice {}: not a SevaDesk donation", invoice.path("id").asText());
            return;
        }
        long paidAt = invoice.path("status_transitions").path("paid_at").asLong(created.getEpochSecond());
        String frequency = meta.path("frequency").asText("");
        String kind = frequency.isBlank() ? "Recurring gift" : (frequency.charAt(0) + frequency.substring(1).toLowerCase() + " gift");
        gifts.record(new OnlineGifts.PaidGift(invoice.path("id").asText(), amount, meta.path("fund").asText(null),
                text(invoice.path("customer_email")), text(invoice.path("customer_name")),
                Instant.ofEpochSecond(paidAt), note(meta, kind)));
    }

    private static String note(JsonNode meta, String kind) {
        List<String> parts = new ArrayList<>();
        parts.add(kind);
        if (!meta.path("label").asText("").isBlank()) parts.add(meta.path("label").asText());
        if (!meta.path("campaign").asText("").isBlank()) parts.add("campaign: " + meta.path("campaign").asText());
        if (!meta.path("occasion").asText("").isBlank()) parts.add("in honor of: " + meta.path("occasion").asText());
        String fee = meta.path("fee_covered").asText("");
        if (!fee.isBlank() && new BigDecimal(fee).signum() > 0) parts.add("donor covered $" + fee + " fees");
        return String.join(" · ", parts);
    }

    private static BigDecimal cents(JsonNode n) {
        return BigDecimal.valueOf(n.asLong(0)).movePointLeft(2);
    }

    /** Stripe ids may be a plain string or an expanded object with an "id". */
    private static String text(JsonNode n) {
        if (n == null || n.isMissingNode() || n.isNull()) return null;
        if (n.isObject()) return n.path("id").asText(null);
        String s = n.asText();
        return s.isBlank() ? null : s;
    }
}
