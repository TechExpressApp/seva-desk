package com.sevadesk.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

/** Password rules, temporary passwords and one-time "set your password" links. */
@Service
class Passwords {

    private static final Logger log = LoggerFactory.getLogger(Passwords.class);

    static final int MIN_LENGTH = 12;
    static final Duration RESET_LINK_VALID = Duration.ofHours(1);
    static final Duration INVITE_LINK_VALID = Duration.ofHours(24);
    static final int MAX_LINKS_PER_HOUR = 3;

    // No 0/O, 1/l/I so temporary passwords can be read out over the phone
    private static final String ALPHABET = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** What happened when a password was (re)issued: a link was emailed, or here is a temporary password. */
    record Issued(boolean emailSent, String temporaryPassword) {}

    private final AppUserRepository users;
    private final PasswordTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final Mailer mailer;

    Passwords(AppUserRepository users, PasswordTokenRepository tokens, PasswordEncoder encoder, Mailer mailer) {
        this.users = users;
        this.tokens = tokens;
        this.encoder = encoder;
        this.mailer = mailer;
    }

    /** @return a problem with the password, or null if it's acceptable */
    static String problemWith(String password, String email) {
        if (password == null || password.length() < MIN_LENGTH) return "Use at least " + MIN_LENGTH + " characters.";
        if (password.length() > 128) return "Use at most 128 characters.";
        if (email != null && password.equalsIgnoreCase(email)) return "Don't use your email address as your password.";
        if (password.chars().distinct().count() < 5) return "Choose a less repetitive password.";
        return null;
    }

    String encode(String password) {
        return encoder.encode(password);
    }

    boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }

    /**
     * Gives a user a way to set their password: an emailed link if email is set up, otherwise a temporary
     * password (shown once to the admin) that must be changed at the next sign-in.
     */
    @Transactional
    Issued issue(AppUser user, PasswordToken.Purpose purpose, String templeName, String baseUrl) {
        if (mailer.enabled()) {
            sendLink(user, purpose, templeName, baseUrl);
            return new Issued(true, null);
        }
        String temporary = temporaryPassword();
        user.changePassword(encoder.encode(temporary), true);
        tokens.expireAllFor(user.getId(), Instant.now());
        return new Issued(false, temporary);
    }

    /** "Forgot password": emails a 1-hour link. Silent when the email is unknown, inactive or asked too often. */
    @Transactional
    void forgot(String email, String templeName, String baseUrl) {
        if (email == null || email.isBlank()) return;
        var user = users.findByEmailIgnoreCase(email.strip()).filter(AppUser::isActive);
        if (user.isEmpty()) return;
        if (tokens.countByUserIdAndCreatedAtAfter(user.get().getId(), Instant.now().minus(Duration.ofHours(1)))
                >= MAX_LINKS_PER_HOUR) {
            log.warn("Too many password reset requests for a user of {}; ignoring", TenantContext.require().slug());
            return;
        }
        sendLink(user.get(), PasswordToken.Purpose.RESET, templeName, baseUrl);
    }

    /** Uses a one-time link to set a new password. @return a problem, or null on success */
    @Transactional
    String reset(String rawToken, String newPassword) {
        var token = rawToken == null ? null : tokens.findByTokenHash(sha256(rawToken)).orElse(null);
        if (token == null || !token.usable()) return "This link has expired or was already used. Please ask for a new one.";
        var user = users.findById(token.getUserId()).filter(AppUser::isActive).orElse(null);
        if (user == null) return "This link has expired or was already used. Please ask for a new one.";
        String problem = problemWith(newPassword, user.getEmail());
        if (problem != null) return problem;
        user.changePassword(encoder.encode(newPassword), false);
        tokens.expireAllFor(user.getId(), Instant.now());
        token.markUsed();
        log.info("Password set via {} link for a user of {}", token.getPurpose(), TenantContext.require().slug());
        return null;
    }

    private void sendLink(AppUser user, PasswordToken.Purpose purpose, String templeName, String baseUrl) {
        tokens.expireAllFor(user.getId(), Instant.now());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration valid = purpose == PasswordToken.Purpose.INVITE ? INVITE_LINK_VALID : RESET_LINK_VALID;
        tokens.save(new PasswordToken(user.getId(), sha256(raw), purpose, Instant.now().plus(valid)));

        String link = baseUrl + "/admin/reset-password?token=" + raw;
        String subject = purpose == PasswordToken.Purpose.INVITE
                ? "Your " + templeName + " SevaDesk login"
                : "Reset your " + templeName + " SevaDesk password";
        String body = purpose == PasswordToken.Purpose.INVITE
                ? """
                  Hare Krishna %s,

                  An account has been created for you in SevaDesk, %s's administration system.
                  Sign-in email: %s

                  Choose your password here (the link works once, for 24 hours):
                  %s

                  """.formatted(user.getDisplayName(), templeName, user.getEmail(), link)
                : """
                  Hare Krishna %s,

                  Someone (hopefully you) asked to reset your SevaDesk password for %s.
                  Choose a new password here (the link works once, for 1 hour):
                  %s

                  If you didn't ask for this, ignore this email; your password stays the same.
                  """.formatted(user.getDisplayName(), templeName, link);
        mailer.send(user.getEmail(), subject, body);
    }

    static String temporaryPassword() {
        var sb = new StringBuilder();
        for (int i = 0; i < 14; i++) {
            if (i == 4 || i == 9) sb.append('-');
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    static String sha256(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
