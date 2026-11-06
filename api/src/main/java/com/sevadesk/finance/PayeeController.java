package com.sevadesk.finance;

import com.sevadesk.platform.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Expenses per person: how much the temple has paid each contact. The counterpart of the Donors screen.
 * Expenses not linked to a contact (older entries) aren't counted. Row-level security keeps it to one temple.
 */
@RestController
@RequestMapping("/api/expenses/by-person")
@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
class PayeeController {

    record PayeeTotal(UUID contactId, String name, String email, String phone, long expenses, BigDecimal total,
                      BigDecimal thisYear, LocalDate firstExpense, LocalDate lastExpense) {}

    record PayeePage(List<PayeeTotal> content, long totalPeople, BigDecimal grandTotal) {}

    private static final String TOTALS = """
            select c.id, trim(c.first_name || ' ' || coalesce(c.last_name, '')) as name, c.email, c.phone,
                   count(e.id) as expenses, sum(e.amount) as total,
                   coalesce(sum(e.amount) filter (where e.spent_on >= ?), 0) as this_year,
                   min(e.spent_on) as first_expense, max(e.spent_on) as last_expense
            from contact c
            join expense e on e.contact_id = c.id
            """;

    private static final String SEARCH = """
            where (? = '%%' or lower(c.first_name || ' ' || coalesce(c.last_name, '')) like ?
                   or lower(coalesce(c.email, '')) like ? or coalesce(c.phone, '') like ?)
            """;

    private final JdbcTemplate jdbc;

    PayeeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** People who were paid, with totals. Search by name, email or phone; sort by any column. */
    @GetMapping
    @Transactional(readOnly = true)
    PayeePage list(@RequestParam(defaultValue = "") String q,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "25") int size,
                   @RequestParam(defaultValue = "total") String sort,
                   @RequestParam(defaultValue = "desc") String dir) {
        int limit = Math.min(Math.max(size, 1), 100);
        String like = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        var rows = jdbc.query(TOTALS + SEARCH + " group by c.id order by " + orderBy(sort, dir) + " limit ? offset ?",
                this::row, yearStart(), like, like, like, like, limit, (long) Math.max(page, 0) * limit);
        var summary = jdbc.queryForMap("""
                select count(*) as people, coalesce(sum(total), 0) as grand_total from (
                  select sum(e.amount) as total from contact c join expense e on e.contact_id = c.id
                """ + SEARCH + " group by c.id) t", like, like, like, like);
        return new PayeePage(rows, ((Number) summary.get("people")).longValue(), (BigDecimal) summary.get("grand_total"));
    }

    /** One person's totals. Zeroes if nothing has been paid to them. */
    @GetMapping("/{contactId}")
    @Transactional(readOnly = true)
    PayeeTotal summary(@PathVariable UUID contactId) {
        var rows = jdbc.query(TOTALS + " where c.id = ? group by c.id", this::row, yearStart(), contactId);
        return rows.isEmpty()
                ? new PayeeTotal(contactId, null, null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null)
                : rows.get(0);
    }

    /** Fixed ORDER BY expressions only; anything else falls back to the total. Ties by name. */
    static String orderBy(String sort, String dir) {
        String direction = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        String column = switch (sort) {
            case "name" -> "lower(trim(c.first_name || ' ' || coalesce(c.last_name, '')))";
            case "email" -> "lower(nullif(c.email, ''))";
            case "expenses" -> "count(e.id)";
            case "thisYear" -> "this_year";
            case "lastExpense" -> "last_expense";
            case "firstExpense" -> "first_expense";
            default -> "total";
        };
        return column + " " + direction + " nulls last, lower(trim(c.first_name || ' ' || coalesce(c.last_name, ''))) asc, c.id";
    }

    private static Date yearStart() {
        return Date.valueOf(LocalDate.now(TenantContext.require().zone()).withDayOfYear(1));
    }

    private PayeeTotal row(ResultSet rs, int i) throws SQLException {
        Date first = rs.getDate("first_expense");
        Date last = rs.getDate("last_expense");
        return new PayeeTotal(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getLong("expenses"), rs.getBigDecimal("total"), rs.getBigDecimal("this_year"),
                first == null ? null : first.toLocalDate(), last == null ? null : last.toLocalDate());
    }
}
