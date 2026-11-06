package com.sevadesk.payments;

import com.sevadesk.platform.TenantContext;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData;
import com.stripe.param.checkout.SessionCreateParams.LineItem.PriceData.Recurring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Starts a Stripe Checkout payment for a website donation. The donor pays on Stripe's own page (card details never
 * touch our servers); Stripe then calls {@link StripeWebhookController}, which records the gift in SevaDesk.
 */
@Service
@EnableConfigurationProperties(StripeAccounts.class)
public class GiftCheckout {

    private static final Logger log = LoggerFactory.getLogger(GiftCheckout.class);

    // Stripe's standard US card pricing, used for the "cover the transaction fee" option.
    private static final BigDecimal FEE_PERCENT = new BigDecimal("0.029");
    private static final BigDecimal FEE_FIXED = new BigDecimal("0.30");

    public enum Frequency {
        ONE_TIME(null, "One time"), WEEKLY(Recurring.Interval.WEEK, "Weekly"),
        MONTHLY(Recurring.Interval.MONTH, "Monthly"), YEARLY(Recurring.Interval.YEAR, "Yearly");

        private final Recurring.Interval interval;
        private final String label;

        Frequency(Recurring.Interval interval, String label) {
            this.interval = interval;
            this.label = label;
        }

        public String label() { return label; }
    }

    /**
     * @param amount       gift amount before any fee cover
     * @param label        what the donor chose, e.g. "Sunday Feast Prasadam Sponsor"
     * @param installments for pledges such as 25 monthly payments; null = until the donor cancels
     */
    public record Gift(BigDecimal amount, boolean coverFees, Frequency frequency, String fundName, String label,
                       String occasion, String email, String campaign, Integer installments) {}

    public static class PaymentsUnavailable extends RuntimeException {
        PaymentsUnavailable(String message) { super(message); }
    }

    private final StripeAccounts accounts;

    GiftCheckout(StripeAccounts accounts) {
        this.accounts = accounts;
    }

    public boolean enabled() {
        return accounts.current().isPresent();
    }

    public boolean testMode() {
        return accounts.current().map(StripeAccounts.Account::testMode).orElse(false);
    }

    /** Extra amount that covers Stripe's fee, so the temple receives the full gift. */
    public static BigDecimal feeFor(BigDecimal amount) {
        BigDecimal gross = amount.add(FEE_FIXED).divide(BigDecimal.ONE.subtract(FEE_PERCENT), 2, RoundingMode.HALF_UP);
        return gross.subtract(amount);
    }

    /** @return the Stripe Checkout URL to send the donor to */
    public String start(Gift gift, String templeName, String baseUrl) {
        var account = accounts.current().orElseThrow(() -> new PaymentsUnavailable("Online giving is not set up yet"));
        var tenant = TenantContext.require();

        BigDecimal fee = gift.coverFees() ? feeFor(gift.amount()) : BigDecimal.ZERO;
        long cents = gift.amount().add(fee).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        String fund = gift.fundName() == null || gift.fundName().isBlank() ? "General" : gift.fundName();
        String name = (gift.label() == null || gift.label().isBlank() ? "Donation" : gift.label())
                + " – " + templeName;
        String description = gift.occasion() == null || gift.occasion().isBlank() ? null : "In honor of: " + gift.occasion();

        var price = PriceData.builder()
                .setCurrency(tenant.currency().toLowerCase(Locale.ROOT))
                .setUnitAmount(cents)
                .setProductData(PriceData.ProductData.builder().setName(name).setDescription(description).build());
        boolean recurring = gift.frequency() != Frequency.ONE_TIME;
        if (recurring) price.setRecurring(Recurring.builder().setInterval(gift.frequency().interval).build());

        var params = SessionCreateParams.builder()
                .setMode(recurring ? SessionCreateParams.Mode.SUBSCRIPTION : SessionCreateParams.Mode.PAYMENT)
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(price.build()).build())
                .setSuccessUrl(baseUrl + "/donate/thank-you?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(baseUrl + "/donate?cancelled")
                .setClientReferenceId(tenant.slug());
        if (gift.email() != null && !gift.email().isBlank()) params.setCustomerEmail(gift.email().trim());

        // Metadata travels with the payment and comes back in the webhook, so the gift lands in the right fund.
        String occasion = gift.occasion() == null ? "" : gift.occasion();
        String coveredFee = fee.toPlainString();
        String campaign = gift.campaign() == null ? "" : gift.campaign();
        String installments = gift.installments() == null ? "" : gift.installments().toString();
        params.putMetadata("tenant", tenant.slug()).putMetadata("fund", fund).putMetadata("label", name)
                .putMetadata("occasion", occasion).putMetadata("fee_covered", coveredFee)
                .putMetadata("campaign", campaign).putMetadata("installments", installments)
                .putMetadata("frequency", gift.frequency().name());
        if (recurring) {
            params.setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                    .setDescription(name)
                    .putMetadata("tenant", tenant.slug()).putMetadata("fund", fund).putMetadata("label", name)
                    .putMetadata("occasion", occasion).putMetadata("fee_covered", coveredFee)
                    .putMetadata("campaign", campaign).putMetadata("installments", installments)
                    .putMetadata("frequency", gift.frequency().name())
                    .build());
        } else {
            params.setSubmitType(SessionCreateParams.SubmitType.DONATE);
            params.setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                    .setDescription(name)
                    .putMetadata("tenant", tenant.slug()).putMetadata("fund", fund)
                    .build());
        }

        try {
            var session = new StripeClient(account.secretKey()).v1().checkout().sessions().create(params.build());
            log.info("Started Stripe checkout {} for {} ({} {})", session.getId(), tenant.slug(), gift.frequency(), cents);
            return session.getUrl();
        } catch (StripeException e) {
            log.warn("Stripe checkout failed for {}: {}", tenant.slug(), e.getMessage());
            String message = "The payment page could not be opened. Please try again in a moment.";
            // In test mode, show staff what Stripe said (e.g. an invalid key); live donors only see the generic text.
            if (account.testMode()) {
                String reason = e.getStripeError() != null && e.getStripeError().getMessage() != null
                        ? e.getStripeError().getMessage() : e.getMessage();
                message += " (Test mode – Stripe said: " + reason + ")";
            }
            throw new PaymentsUnavailable(message);
        }
    }

    /** Ends a fixed-length pledge (e.g. 25 monthly payments) after its last instalment. */
    void limitInstallments(String subscriptionId, int installments, Frequency frequency, long startEpochSecond) {
        var account = accounts.current().orElseThrow();
        var start = java.time.Instant.ofEpochSecond(startEpochSecond).atZone(java.time.ZoneOffset.UTC);
        var end = switch (frequency) {
            case WEEKLY -> start.plusWeeks(installments);
            case YEARLY -> start.plusYears(installments);
            default -> start.plusMonths(installments);
        };
        // cancel just before the payment after the last instalment would be taken
        long cancelAt = end.minusHours(12).toEpochSecond();
        try {
            new StripeClient(account.secretKey()).v1().subscriptions().update(subscriptionId,
                    com.stripe.param.SubscriptionUpdateParams.builder().setCancelAt(cancelAt).build());
            log.info("Pledge {} set to end after {} payments", subscriptionId, installments);
        } catch (StripeException e) {
            log.error("Could not set the end date on pledge {}: {}", subscriptionId, e.getMessage());
        }
    }
}
