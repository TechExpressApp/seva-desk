package com.sevadesk.finance;

import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.platform.TenantContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/** Expenses: who was paid (a contact), when, how much and for what purpose. Treasurers and admins only. */
@RestController
@RequestMapping("/api/expenses")
@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
class ExpenseController {

    record ExpenseForm(@NotNull(message = "Choose who was paid") UUID contactId,
                       @NotNull(message = "Choose the date") LocalDate spentOn,
                       @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
                       @NotNull(message = "Choose a purpose") ExpensePurpose purpose,
                       @Size(max = 500) String notes) {}

    record ExpenseView(UUID id, UUID contactId, String name, LocalDate spentOn, BigDecimal amount,
                       ExpensePurpose purpose, String purposeLabel, String notes) {}

    record PurposeOption(ExpensePurpose value, String label) {}

    record PurposeTotal(ExpensePurpose purpose, String label, BigDecimal amount) {}

    record Summary(BigDecimal thisMonth, BigDecimal thisYear, List<PurposeTotal> thisMonthByPurpose) {}

    private final ExpenseRepository expenses;
    private final ExpenseService service;
    private final ContactRepository contacts;
    private final JdbcTemplate jdbc;

    ExpenseController(ExpenseRepository expenses, ExpenseService service, ContactRepository contacts, JdbcTemplate jdbc) {
        this.expenses = expenses;
        this.service = service;
        this.contacts = contacts;
        this.jdbc = jdbc;
    }

    @GetMapping
    @Transactional(readOnly = true)
    Page<ExpenseView> list(@RequestParam(required = false) ExpensePurpose purpose,
                           @RequestParam(required = false) UUID contactId,
                           @RequestParam(required = false) LocalDate from,
                           @RequestParam(required = false) LocalDate to,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "25") int size) {
        var result = expenses.search(purpose, contactId, from, to, PageRequest.of(Math.max(page, 0), Math.min(size, 100)));
        var names = names(result.getContent());
        return result.map(e -> view(e, names));
    }

    @GetMapping("/purposes")
    List<PurposeOption> purposes() {
        return Arrays.stream(ExpensePurpose.values()).map(p -> new PurposeOption(p, p.label())).toList();
    }

    /** This month and this year (calendar, in the temple's time zone), and this month by purpose. */
    @GetMapping("/summary")
    @Transactional(readOnly = true)
    Summary summary() {
        LocalDate today = LocalDate.now(TenantContext.require().zone());
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal month = jdbc.queryForObject("select coalesce(sum(amount), 0) from expense where spent_on >= ?",
                BigDecimal.class, monthStart);
        BigDecimal year = jdbc.queryForObject("select coalesce(sum(amount), 0) from expense where spent_on >= ?",
                BigDecimal.class, today.withDayOfYear(1));
        var byPurpose = jdbc.query("""
                select purpose, sum(amount) from expense where spent_on >= ? group by purpose order by 2 desc
                """, (rs, i) -> {
                    var p = ExpensePurpose.valueOf(rs.getString(1));
                    return new PurposeTotal(p, p.label(), rs.getBigDecimal(2));
                }, monthStart);
        return new Summary(month, year, byPurpose);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    ExpenseView create(@Valid @RequestBody ExpenseForm f) {
        var contact = contact(f.contactId());
        var e = service.record(new Expense(contact.getId(), f.purpose(), f.amount(), f.spentOn(), f.notes()));
        return view(e, Map.of(contact.getId(), contact.getFullName()));
    }

    @PutMapping("/{id}")
    @Transactional
    ExpenseView update(@PathVariable UUID id, @Valid @RequestBody ExpenseForm f) {
        var contact = contact(f.contactId());
        var e = find(id);
        e.update(contact.getId(), f.purpose(), f.amount(), f.spentOn(), f.notes());
        service.changed(e);
        return view(e, Map.of(contact.getId(), contact.getFullName()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    void delete(@PathVariable UUID id) {
        var e = find(id);
        expenses.delete(e);
        service.changed(e);
    }

    private Expense find(UUID id) {
        return expenses.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
    }

    private Contact contact(UUID id) {
        return contacts.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose who was paid from your contacts."));
    }

    private Map<UUID, String> names(List<Expense> list) {
        var ids = list.stream().map(Expense::getContactId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return contacts.findAllById(ids).stream().collect(Collectors.toMap(Contact::getId, Contact::getFullName));
    }

    private static ExpenseView view(Expense e, Map<UUID, String> names) {
        String name = e.getContactId() == null ? "—" : names.getOrDefault(e.getContactId(), "—");
        return new ExpenseView(e.getId(), e.getContactId(), name, e.getSpentOn(), e.getAmount(), e.getPurpose(),
                e.getPurpose().label(), e.getNotes());
    }
}
