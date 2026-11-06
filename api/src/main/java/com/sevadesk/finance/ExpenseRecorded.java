package com.sevadesk.finance;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseRecorded(UUID tenantId, UUID expenseId, String category, BigDecimal amount) {}
