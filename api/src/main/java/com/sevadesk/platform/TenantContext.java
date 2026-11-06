package com.sevadesk.platform;

import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The tenant (temple) the current request or job is working for.
 * Set by {@link TenantResolverFilter} for web requests; jobs and seeders set it with {@link #runAs}.
 */
public final class TenantContext {

    public record TenantRef(UUID id, String slug, String shortName, String currency, ZoneId zone, int fiscalYearStart) {}

    private static final ThreadLocal<TenantRef> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static TenantRef current() {
        return CURRENT.get();
    }

    public static TenantRef require() {
        TenantRef ref = CURRENT.get();
        if (ref == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return ref;
    }

    public static UUID currentId() {
        TenantRef ref = CURRENT.get();
        return ref == null ? null : ref.id();
    }

    static void set(TenantRef ref) {
        CURRENT.set(ref);
    }

    static void clear() {
        CURRENT.remove();
    }

    /** Runs work on behalf of a tenant, restoring the previous tenant afterwards. */
    public static <T> T runAs(TenantRef ref, Supplier<T> work) {
        TenantRef previous = CURRENT.get();
        CURRENT.set(ref);
        try {
            return work.get();
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    public static void runAs(TenantRef ref, Runnable work) {
        runAs(ref, () -> { work.run(); return null; });
    }
}
