package com.sevadesk.reporting;

import com.sevadesk.platform.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/** Live totals for the dashboard. Plain SQL; row-level security scopes it to the current temple. */
@Service
public class DashboardService {

    public record FundTotal(String fund, BigDecimal amount) {}

    public record Summary(String currency, BigDecimal todayDonations, long todayCount, BigDecimal monthDonations,
                          BigDecimal monthExpenses, BigDecimal monthNet, List<FundTotal> monthByFund) {}

    private final JdbcTemplate jdbc;

    DashboardService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** REQUIRES_NEW so it can run from an after-commit listener with a fresh connection. */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Summary summary() {
        var tenant = TenantContext.require();
        LocalDate today = LocalDate.now(tenant.zone());
        LocalDate monthStart = today.withDayOfMonth(1);
        var todayStart = Timestamp.from(today.atStartOfDay(tenant.zone()).toInstant());
        var monthStartTs = Timestamp.from(monthStart.atStartOfDay(tenant.zone()).toInstant());

        var donations = jdbc.queryForMap("""
                select coalesce(sum(amount) filter (where donated_at >= ?), 0) as today_amount,
                       count(*)             filter (where donated_at >= ?)     as today_count,
                       coalesce(sum(amount), 0)                                 as month_amount
                from donation where status = 'RECORDED' and donated_at >= ?
                """, todayStart, todayStart, monthStartTs);

        BigDecimal monthExpenses = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from expense where spent_on >= ?", BigDecimal.class, monthStart);

        List<FundTotal> byFund = jdbc.query("""
                select f.name, sum(d.amount) as total
                from donation d join fund f on f.id = d.fund_id
                where d.status = 'RECORDED' and d.donated_at >= ?
                group by f.name order by total desc
                """, (rs, i) -> new FundTotal(rs.getString(1), rs.getBigDecimal(2)), monthStartTs);

        BigDecimal monthDonations = (BigDecimal) donations.get("month_amount");
        return new Summary(tenant.currency(),
                (BigDecimal) donations.get("today_amount"),
                ((Number) donations.get("today_count")).longValue(),
                monthDonations, monthExpenses, monthDonations.subtract(monthExpenses), byFund);
    }
}
