package com.sevadesk.website;

import com.sevadesk.payments.GiftCheckout;
import com.sevadesk.payments.GiftCheckout.Frequency;
import com.sevadesk.platform.Tenant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * The /donate page and the hand-off to Stripe Checkout. Shares the website's model (temple, site, colours)
 * through {@link WebsiteModel}.
 */
@Controller
class DonateController {

    static final BigDecimal MIN = BigDecimal.ONE;
    static final BigDecimal MAX = new BigDecimal("50000");

    private final GiftCheckout checkout;
    private final CampaignProgress progress;

    DonateController(GiftCheckout checkout, CampaignProgress progress) {
        this.checkout = checkout;
        this.progress = progress;
    }

    @GetMapping("/donate")
    String donate(Model model, @RequestParam(required = false) String cancelled,
                  @RequestParam(required = false) String error) {
        var site = (SiteSettings) model.getAttribute("site");
        model.addAttribute("page", site.pages().get("donate"));
        model.addAttribute("donate", site.donate());
        model.addAttribute("onlineEnabled", checkout.enabled());
        model.addAttribute("testMode", checkout.testMode());
        if (site.donate() != null) model.addAttribute("campaignProgress", progress.of(site.donate().campaigns()));
        model.addAttribute("cancelled", cancelled != null);
        model.addAttribute("error", error);
        return "site/donate";
    }

    @PostMapping("/donate/checkout")
    String startCheckout(Model model,
                         @RequestParam(defaultValue = "ONE_TIME") String frequency,
                         @RequestParam(required = false) String option,
                         @RequestParam(required = false) String otherAmount,
                         @RequestParam(required = false) String occasion,
                         @RequestParam(defaultValue = "false") boolean coverFees,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String campaign,
                         @RequestParam(required = false) String campaignLevel,
                         @RequestParam(required = false) String campaignAmount,
                         @RequestParam(required = false) String from) {
        // a giving form on another page (e.g. /new-temple-project) goes back there with any error
        String back = from != null && from.matches("/[a-z0-9][a-z0-9-]{0,60}") && !from.equals("/donate") ? from : null;
        var site = (SiteSettings) model.getAttribute("site");
        var temple = (Tenant) model.getAttribute("temple");
        var donate = site.donate();
        if (donate == null) return "redirect:/donate";

        String note = occasion == null ? null : occasion.strip();
        if (note != null && note.length() > 100) note = note.substring(0, 100);
        String mail = email == null || email.isBlank() || !email.contains("@") ? null : email.strip();

        GiftCheckout.Gift gift;
        if (campaign != null && !campaign.isBlank()) {
            var c = donate.campaigns().stream().filter(x -> campaign.equals(x.key())).findFirst().orElse(null);
            if (c == null) return error(back, campaign, "Please choose a donation option.");
            BigDecimal amount;
            String label = c.title();
            if (c.amount() != null) {
                amount = c.amount();
            } else if (!c.levels().isEmpty() && campaignLevel != null && !"other".equals(campaignLevel)) {
                int i;                                   // a level such as "Platinum Patron – $216"
                try {
                    i = Integer.parseInt(campaignLevel);
                } catch (NumberFormatException e) {
                    return error(back, campaign, "Please choose a donation option.");
                }
                if (i < 0 || i >= c.levels().size()) return error(back, campaign, "Please choose a donation option.");
                amount = c.levels().get(i).amount();
                label = c.title() + " – " + c.levels().get(i).label();
            } else {
                amount = money(campaignAmount);          // any amount the donor likes
            }
            if (amount == null) return error(back, campaign, "Please enter an amount between $1 and $50,000.");
            Frequency f = parse(c.frequency(), Frequency.MONTHLY);
            if (!c.frequencies().isEmpty()) {                 // the donor chooses how often
                f = parse(frequency, null);
                if (f == null || !c.frequencies().contains(f.name())) return error(back, campaign, "Please choose how often to give.");
            }
            gift = new GiftCheckout.Gift(amount, coverFees, f, c.fund(), label, note, mail, c.key(), c.installments());
        } else {
            Frequency f = parse(frequency, null);
            if (f == null || !donate.frequencies().contains(f.name())) return error("Please choose how often to give.");
            BigDecimal amount;
            String label;
            String fund = donate.defaultFund();
            if ("other".equals(option) || option == null || option.isBlank()) {
                if (!donate.allowOther()) return error("Please choose a donation option.");
                amount = money(otherAmount);
                label = "Donation";
            } else {
                int i;
                try {
                    i = Integer.parseInt(option);
                } catch (NumberFormatException e) {
                    return error("Please choose a donation option.");
                }
                if (i < 0 || i >= donate.options().size()) return error("Please choose a donation option.");
                var o = donate.options().get(i);
                amount = o.amount();
                label = o.label();
                if (o.fund() != null) fund = o.fund();
            }
            if (amount == null) return error("Please enter an amount between $1 and $50,000.");
            gift = new GiftCheckout.Gift(amount, coverFees && donate.coverFees(), f, fund, label, note, mail, null, null);
        }

        try {
            String url = checkout.start(gift, temple.getName(), (String) model.getAttribute("baseUrl"));
            return "redirect:" + url;
        } catch (GiftCheckout.PaymentsUnavailable e) {
            return error(e.getMessage());
        }
    }

    @GetMapping("/donate/thank-you")
    String thankYou(Model model) {
        var site = (SiteSettings) model.getAttribute("site");
        model.addAttribute("donate", site.donate());
        return "site/thank-you";
    }

    /** "$1,001.50" or "1001.5" → 1001.50; null if missing or out of range. */
    static BigDecimal money(String text) {
        if (text == null) return null;
        String cleaned = text.replaceAll("[\\s$,]", "");
        if (!cleaned.matches("\\d{1,6}(\\.\\d{1,2})?")) return null;
        BigDecimal v = new BigDecimal(cleaned).setScale(2);
        return v.compareTo(MIN) < 0 || v.compareTo(MAX) > 0 ? null : v;
    }

    private static Frequency parse(String value, Frequency fallback) {
        if (value == null) return fallback;
        try {
            return Frequency.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private static String error(String message) {
        return "redirect:/donate?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8) + "#give";
    }

    /** Back to the page the appeal's form was on (or /donate), at the appeal, with the message. */
    private static String error(String back, String campaign, String message) {
        if (back == null) return error(message);
        String anchor = campaign != null && campaign.matches("[a-z0-9-]{1,60}") ? "#" + campaign : "";
        return "redirect:" + back + "?error=" + URLEncoder.encode(message, StandardCharsets.UTF_8) + anchor;
    }
}
