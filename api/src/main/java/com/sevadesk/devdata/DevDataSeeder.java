package com.sevadesk.devdata;

import com.sevadesk.contacts.Contact;
import com.sevadesk.contacts.ContactRepository;
import com.sevadesk.donors.Donation;
import com.sevadesk.donors.DonationService;
import com.sevadesk.donors.Fund;
import com.sevadesk.donors.FundRepository;
import com.sevadesk.finance.Expense;
import com.sevadesk.finance.ExpenseService;
import com.sevadesk.platform.AppUser;
import com.sevadesk.platform.AppUserRepository;
import com.sevadesk.platform.TenantContext;
import com.sevadesk.platform.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Fills each demo tenant with a login, funds, contacts, donations and expenses. Dev profile only. */
@Component
@Profile("dev")
class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final TenantRepository tenants;
    private final AppUserRepository users;
    private final FundRepository funds;
    private final ContactRepository contacts;
    private final DonationService donations;
    private final ExpenseService expenses;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    DevDataSeeder(TenantRepository tenants, AppUserRepository users, FundRepository funds, ContactRepository contacts,
                  DonationService donations, ExpenseService expenses, PasswordEncoder passwordEncoder,
                  @Value("${sevadesk.dev.admin-email}") String adminEmail,
                  @Value("${sevadesk.dev.admin-password}") String adminPassword) {
        this.tenants = tenants;
        this.users = users;
        this.funds = funds;
        this.contacts = contacts;
        this.donations = donations;
        this.expenses = expenses;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (var tenant : tenants.findByActiveTrue()) {
            TenantContext.runAs(tenant.toRef(), () -> {
                if (users.count() > 0) return;
                log.info("Seeding demo data for {}", tenant.getName());
                users.save(new AppUser(adminEmail, passwordEncoder.encode(adminPassword), "Temple Admin", List.of("ADMIN")));

                var general = funds.save(new Fund("General", true));
                var food = funds.save(new Fund(tenant.getSlug().equals("gurunanak") ? "Langar" : "Annadanam", true));
                var building = funds.save(new Fund("Building Fund", true));

                var people = List.of(
                        contact("Lakshmi", "Narayanan", "Bengaluru", "Kashyapa", "Rohini"),
                        contact("Priya", "Rao", "Whitefield", "Bharadwaja", "Ashwini"),
                        contact("Harpreet", "Singh", "Indiranagar", null, null),
                        contact("Arjun", "Menon", "Koramangala", "Vasishta", "Pushya"));

                Instant now = Instant.now();
                record Gift(int person, Fund fund, int amount, Donation.PaymentMode mode, long hoursAgo) {}
                for (var g : List.of(
                        new Gift(0, food, 2501, Donation.PaymentMode.UPI, 1),
                        new Gift(1, general, 1001, Donation.PaymentMode.CASH, 3),
                        new Gift(2, building, 11000, Donation.PaymentMode.BANK_TRANSFER, 30),
                        new Gift(3, food, 5000, Donation.PaymentMode.CARD, 50),
                        new Gift(-1, general, 18640, Donation.PaymentMode.CASH, 2))) {
                    donations.record(new DonationService.NewDonation(
                            g.person() < 0 ? null : people.get(g.person()).getId(), g.fund().getId(),
                            BigDecimal.valueOf(g.amount()), g.mode(), null,
                            now.minus(g.hoursAgo(), ChronoUnit.HOURS), g.person() < 0 ? "Hundi count, main hall" : null));
                }

                LocalDate today = LocalDate.now(tenant.toRef().zone());
                expenses.record(new Expense("Flowers & decoration", general.getId(), new BigDecimal("4200"), today, "Malli Florists", null));
                expenses.record(new Expense("Groceries", food.getId(), new BigDecimal("12850"), today.withDayOfMonth(1), "Sri Balaji Stores", null));
            });
        }
    }

    private Contact contact(String first, String last, String city, String gotra, String nakshatra) {
        var c = new Contact(first, last);
        c.setCity(city);
        c.setGotra(gotra);
        c.setNakshatra(nakshatra);
        c.setPhone("+91 98450 " + (10000 + Math.abs((first + last).hashCode()) % 89999));
        c.setConsentEmail(true);
        return contacts.save(c);
    }
}
