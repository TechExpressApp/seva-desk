package com.sevadesk.payments;

import com.sevadesk.platform.TenantContext;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Optional;

/**
 * Each temple's own Stripe account, keyed by temple slug. Money goes straight to the temple, never through SevaDesk.
 * Set from environment variables, e.g. for the "iskconsomerset" temple:
 * <pre>
 * SEVADESK_STRIPE_ACCOUNTS_ISKCONSOMERSET_SECRETKEY=sk_live_...
 * SEVADESK_STRIPE_ACCOUNTS_ISKCONSOMERSET_WEBHOOKSECRET=whsec_...
 * </pre>
 */
@ConfigurationProperties("sevadesk.stripe")
public record StripeAccounts(Map<String, Account> accounts) {

    public record Account(String secretKey, String webhookSecret) {
        boolean usable() {
            return secretKey != null && !secretKey.isBlank() && webhookSecret != null && !webhookSecret.isBlank();
        }

        boolean testMode() {
            return secretKey != null && secretKey.startsWith("sk_test_");
        }
    }

    public StripeAccounts {
        accounts = accounts == null ? Map.of() : accounts;
    }

    /** The current temple's Stripe account, if both keys are configured. */
    public Optional<Account> current() {
        var tenant = TenantContext.current();
        if (tenant == null) return Optional.empty();
        return Optional.ofNullable(accounts.get(tenant.slug())).filter(Account::usable);
    }
}
