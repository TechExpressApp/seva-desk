package com.sevadesk.website;

import com.sevadesk.donors.FundTotals;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Progress towards an appeal's goal: what was raised before SevaDesk plus every recorded gift to the appeal's
 * fund. Shown as "$21,041 raised of $500,000 · 37 donations".
 */
@Component
class CampaignProgress {

    record Progress(BigDecimal raised, long donations, BigDecimal goal, int percent) {}

    private final FundTotals totals;

    CampaignProgress(FundTotals totals) {
        this.totals = totals;
    }

    /** Progress for each appeal that has a goal, by appeal key. */
    Map<String, Progress> of(List<SiteSettings.Campaign> campaigns) {
        Map<String, Progress> out = new HashMap<>();
        for (var c : campaigns) {
            if (c.goal() == null || c.goal().signum() <= 0) continue;
            var t = totals.forFund(c.fund());
            BigDecimal raised = t.amount().add(c.raisedBefore() == null ? BigDecimal.ZERO : c.raisedBefore());
            long count = t.donations() + (c.donationsBefore() == null ? 0 : c.donationsBefore());
            int percent = raised.multiply(BigDecimal.valueOf(100)).divide(c.goal(), 0, RoundingMode.DOWN).min(BigDecimal.valueOf(100)).intValue();
            out.put(c.key(), new Progress(raised, count, c.goal(), percent));
        }
        return out;
    }
}
