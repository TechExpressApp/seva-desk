package com.sevadesk.finance;

import com.sevadesk.platform.TenantContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpenseService {

    private final ExpenseRepository expenses;
    private final ApplicationEventPublisher events;

    ExpenseService(ExpenseRepository expenses, ApplicationEventPublisher events) {
        this.expenses = expenses;
        this.events = events;
    }

    @Transactional
    public Expense record(Expense expense) {
        var saved = expenses.save(expense);
        changed(saved);
        return saved;
    }

    /** Call after changing an expense inside a transaction, so live totals refresh. */
    void changed(Expense e) {
        events.publishEvent(new ExpenseRecorded(TenantContext.require().id(), e.getId(), e.getPurpose(), e.getAmount()));
    }
}
