package com.sevadesk.finance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
@PreAuthorize("hasAnyRole('ADMIN','TREASURER')")
class ExpenseController {

    record ExpenseForm(@NotBlank @Size(max = 60) String category, UUID fundId,
                       @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal amount,
                       @NotNull LocalDate spentOn, @Size(max = 150) String payee, @Size(max = 500) String notes) {}

    record ExpenseView(UUID id, String category, UUID fundId, BigDecimal amount, LocalDate spentOn, String payee, String notes) {
        static ExpenseView of(Expense e) {
            return new ExpenseView(e.getId(), e.getCategory(), e.getFundId(), e.getAmount(), e.getSpentOn(), e.getPayee(), e.getNotes());
        }
    }

    private final ExpenseService service;
    private final ExpenseRepository expenses;

    ExpenseController(ExpenseService service, ExpenseRepository expenses) {
        this.service = service;
        this.expenses = expenses;
    }

    @GetMapping
    Page<ExpenseView> list(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int size) {
        return expenses.findAllByOrderBySpentOnDescCreatedAtDesc(PageRequest.of(page, Math.min(size, 100))).map(ExpenseView::of);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseView create(@Valid @RequestBody ExpenseForm f) {
        return ExpenseView.of(service.record(new Expense(f.category(), f.fundId(), f.amount(), f.spentOn(), f.payee(), f.notes())));
    }
}
