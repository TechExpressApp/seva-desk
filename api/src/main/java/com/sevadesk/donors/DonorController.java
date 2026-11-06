package com.sevadesk.donors;

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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Total giving per person. Every donation linked to a contact counts; voided donations and anonymous gifts don't.
 * Row-level security limits all queries to the current temple.
 */
@RestController
@RequestMapping("/api/donors")
@PreAuthorize("hasAnyRole('ADMIN','TREASURER','FRONT_DESK')")
class DonorController {

    record DonorTotal(UUID contactId, String name, String email, String phone, long gifts, BigDecimal total,
                      BigDecimal thisYear, Instant firstGift, Instant lastGift) {}

    record DonorPage(List<DonorTotal> content, long totalDonors, BigDecimal grandTotal) {}

    private static final String TOTALS = """
            select c.id, trim(c.first_name || ' ' || coalesce(c.last_name, '')) as name, c.email, c.phone,
                   count(d.id) as gifts, sum(d.amount) as total,
                   coalesce(sum(d.amount) filter (where d.donated_at >= ?), 0) as this_year,
                   min(d.donated_at) as first_gift, max(d.donated_at) as last_gift
            from contact c
            join donation d on d.contact_id = c.id and d.status = 'RECORDED'
            """;

    private final JdbcTemplate jdbc;

    DonorController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Donors with their totals, biggest givers first. Search by name, email or phone. */
    @GetMapping
    @Transactional(readOnly = true)
    DonorPage list(@RequestParam(defaultValue = "") String q,
                   @RequestParam(defaultValue = "0") int page,
                   @RequestParam(defaultValue = "25") int size,
                   @RequestParam(defaultValue = "total") String sort,
                   @RequestParam(defaultValue = "desc") String dir) {
        int limit = Math.min(Math.max(size, 1), 100);
        String like = "%" + q.strip().toLowerCase(Locale.ROOT) + "%";
        String where = """
                where (? = '%%' or lower(c.first_name || ' ' || coalesce(c.last_name, '')) like ?
                       or lower(coalesce(c.email, '')) like ? or coalesce(c.phone, '') like ?)
                """;
        var rows = jdbc.query(TOTALS + where + " group by c.id order by " + orderBy(sort, dir) + " limit ? offset ?",
                this::row, yearStart(), like, like, like, like, limit, (long) Math.max(page, 0) * limit);

        var summary = jdbc.queryForMap("""
                select count(*) as donors, coalesce(sum(total), 0) as grand_total from (
                  select sum(d.amount) as total from contact c
                  join donation d on d.contact_id = c.id and d.status = 'RECORDED'
                """ + where + " group by c.id) t", like, like, like, like);
        return new DonorPage(rows, ((Number) summary.get("donors")).longValue(), (BigDecimal) summary.get("grand_total"));
    }

    /** One person's giving, for the contact screen. Zeroes if they haven't given yet. */
    @GetMapping("/{contactId}")
    @Transactional(readOnly = true)
    DonorTotal summary(@PathVariable UUID contactId) {
        var rows = jdbc.query(TOTALS + " where c.id = ? group by c.id", this::row, yearStart(), contactId);
        return rows.isEmpty()
                ? new DonorTotal(contactId, null, null, null, 0, BigDecimal.ZERO, BigDecimal.ZERO, null, null)
                : rows.get(0);
    }

    /**
     * ORDER BY for a column of the Donors screen. Only these fixed expressions can reach the SQL, never the request
     * text. Empty values (no email, no gift this year) sort last either way; ties go by name so pages don't shuffle.
     */
    static String orderBy(String sort, String dir) {
        String direction = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        String column = switch (sort) {
            case "name" -> "lower(trim(c.first_name || ' ' || coalesce(c.last_name, '')))";
            case "email" -> "lower(nullif(c.email, ''))";
            case "phone" -> "nullif(regexp_replace(coalesce(c.phone, ''), '[^0-9]', '', 'g'), '')";
            case "gifts" -> "count(d.id)";
            case "thisYear" -> "this_year";
            case "firstGift" -> "first_gift";
            case "lastGift", "recent" -> "last_gift";
            default -> "total";
        };
        String nameTieBreak = "lower(trim(c.first_name || ' ' || coalesce(c.last_name, ''))) asc, c.id";
        return column + " " + direction + " nulls last, " + nameTieBreak;
    }

    /** 1 January of the current year in the temple's time zone. */
    private static Timestamp yearStart() {
        var zone = TenantContext.require().zone();
        return Timestamp.from(LocalDate.now(zone).withDayOfYear(1).atStartOfDay(zone).toInstant());
    }

    private DonorTotal row(ResultSet rs, int i) throws SQLException {
        return new DonorTotal(rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("email"),
                rs.getString("phone"), rs.getLong("gifts"), rs.getBigDecimal("total"), rs.getBigDecimal("this_year"),
                instant(rs.getTimestamp("first_gift")), instant(rs.getTimestamp("last_gift")));
    }

    private static Instant instant(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
