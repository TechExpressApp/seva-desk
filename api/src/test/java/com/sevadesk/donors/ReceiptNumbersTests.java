package com.sevadesk.donors;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ReceiptNumbersTests {

    @Test
    void indianFiscalYearRunsAprilToMarch() {
        assertThat(ReceiptNumbers.fiscalYear(LocalDate.of(2026, 11, 5), 4)).isEqualTo("2026-27");
        assertThat(ReceiptNumbers.fiscalYear(LocalDate.of(2027, 3, 31), 4)).isEqualTo("2026-27");
        assertThat(ReceiptNumbers.fiscalYear(LocalDate.of(2027, 4, 1), 4)).isEqualTo("2027-28");
        assertThat(ReceiptNumbers.fiscalYear(LocalDate.of(2099, 6, 1), 4)).isEqualTo("2099-00");
    }

    @Test
    void calendarYearTemples() {
        assertThat(ReceiptNumbers.fiscalYear(LocalDate.of(2026, 11, 5), 1)).isEqualTo("2026");
    }
}
