package com.sevadesk.donors;

import com.sevadesk.platform.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Gap-free receipt numbers per temple and fiscal year, e.g. {@code RG/2026-27/00042}.
 * The upsert locks the counter row until the surrounding transaction commits, so two front-desk
 * clerks saving at the same moment get consecutive numbers, and a rolled-back save releases its number.
 */
@Component
class ReceiptNumbers {

    private final JdbcTemplate jdbc;

    ReceiptNumbers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    String next(Instant donatedAt) {
        var tenant = TenantContext.require();
        String fy = fiscalYear(LocalDate.ofInstant(donatedAt, tenant.zone()), tenant.fiscalYearStart());
        Integer n = jdbc.queryForObject("""
                insert into receipt_counter (tenant_id, fiscal_year, last_no) values (?, ?, 1)
                on conflict (tenant_id, fiscal_year) do update set last_no = receipt_counter.last_no + 1
                returning last_no
                """, Integer.class, tenant.id(), fy);
        return "%s/%s/%05d".formatted(tenant.shortName(), fy, n);
    }

    /** Fiscal year label. India (April start): 5 Nov 2026 → "2026-27". Calendar-year temples (start 1) → "2026". */
    static String fiscalYear(LocalDate date, int startMonth) {
        if (startMonth == 1) return String.valueOf(date.getYear());
        int startYear = date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
        return "%d-%02d".formatted(startYear, (startYear + 1) % 100);
    }
}
