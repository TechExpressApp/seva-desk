package com.sevadesk.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends plain-text emails (password links). Works only when SMTP is configured; otherwise {@link #enabled()} is
 * false and admins hand out temporary passwords instead. Any SMTP service works: Google Workspace, Brevo, Amazon SES.
 */
@Component
public class Mailer {

    private static final Logger log = LoggerFactory.getLogger(Mailer.class);

    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    private final boolean logLinks;

    Mailer(ObjectProvider<JavaMailSender> sender, @Value("${sevadesk.mail.from:}") String from,
           @Value("${sevadesk.mail.log-links:false}") boolean logLinks) {
        this.sender = sender;
        this.from = from;
        this.logLinks = logLinks;
    }

    public boolean enabled() {
        return sender.getIfAvailable() != null && from != null && !from.isBlank();
    }

    /** @return true if the email was handed to the mail server */
    public boolean send(String to, String subject, String text) {
        if (logLinks) log.info("[dev] Email to {}: {}\n{}", to, subject, text);
        if (!enabled()) return false;
        try {
            var message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            sender.getObject().send(message);
            return true;
        } catch (MailException e) {
            log.error("Could not send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}
