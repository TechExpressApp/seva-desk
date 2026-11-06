package com.sevadesk.finance;

import java.math.BigDecimal;
import java.util.UUID;

/** Published when an expense is added, changed or removed, so live totals (dashboard) refresh. */
public record ExpenseRecorded(UUID tenantId, UUID expenseId, ExpensePurpose purpose, BigDecimal amount) {}
