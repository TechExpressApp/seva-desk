package com.sevadesk.donors;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** How much has been given to a fund: the number of gifts and their total. Voided gifts don't count. */
@Service
public class FundTotals {

    public record Totals(long donations, BigDecimal amount) {
        public static final Totals NONE = new Totals(0, BigDecimal.ZERO);
    }

    private final EntityManager em;

    FundTotals(EntityManager em) {
        this.em = em;
    }

    /** Totals for the fund with this name (ignoring case); zero if there is no such fund yet. */
    @Transactional(readOnly = true)
    public Totals forFund(String fundName) {
        if (fundName == null || fundName.isBlank()) return Totals.NONE;
        Object[] row = (Object[]) em.createQuery("""
                select count(d), coalesce(sum(d.amount), 0) from Donation d, Fund f
                where d.fundId = f.id and lower(f.name) = lower(:name)
                  and d.status = com.sevadesk.donors.Donation.Status.RECORDED
                """).setParameter("name", fundName.strip()).getSingleResult();
        return new Totals(((Number) row[0]).longValue(), (BigDecimal) row[1]);
    }
}
