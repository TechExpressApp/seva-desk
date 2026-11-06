package com.sevadesk.finance;

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
        events.publishEvent(new ExpenseRecorded(saved.getTenantId(), saved.getId(), saved.getCategory(), saved.getAmount()));
        return saved;
    }
}
