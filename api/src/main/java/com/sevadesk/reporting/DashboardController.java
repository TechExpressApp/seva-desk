package com.sevadesk.reporting;

import com.sevadesk.donors.DonationRecorded;
import com.sevadesk.finance.ExpenseRecorded;
import com.sevadesk.platform.TenantContext;
import org.springframework.http.MediaType;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/api/dashboard")
class DashboardController {

    /** Pushed to the browser for each new donation so it can show a "live donations" feed. */
    record DonationTick(String receiptNo, java.math.BigDecimal amount, String fund, String donor, String mode, boolean voided) {}

    private final DashboardService dashboard;
    private final DashboardStream stream;

    DashboardController(DashboardService dashboard, DashboardStream stream) {
        this.dashboard = dashboard;
        this.stream = stream;
    }

    @GetMapping("/summary")
    DashboardService.Summary summary() {
        return dashboard.summary();
    }

    /** Server-Sent Events: "summary" with fresh totals, "donation" for each new gift. */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() throws IOException {
        var emitter = stream.open(TenantContext.require().id());
        emitter.send(SseEmitter.event().name("summary").data(dashboard.summary()));
        return emitter;
    }

    @TransactionalEventListener
    void on(DonationRecorded e) {
        stream.send(e.tenantId(), "donation",
                new DonationTick(e.receiptNo(), e.amount(), e.fundName(), e.donorName(), e.paymentMode(), e.voided()));
        stream.send(e.tenantId(), "summary", dashboard.summary());
    }

    @TransactionalEventListener
    void on(ExpenseRecorded e) {
        stream.send(e.tenantId(), "summary", dashboard.summary());
    }
}
