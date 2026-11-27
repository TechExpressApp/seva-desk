package com.sevadesk.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.assertj.core.api.Assertions.assertThat;

/** Email counts as set up only with a mail server host and a From address. */
class MailerTests {

    @Test
    void aBlankHostMeansEmailIsOff() {
        assertThat(mailer(sender(""), "SevaDesk <no-reply@example.org>").enabled()).isFalse();
        assertThat(mailer(sender(null), "SevaDesk <no-reply@example.org>").enabled()).isFalse();
        assertThat(mailer(null, "SevaDesk <no-reply@example.org>").enabled()).isFalse();
        assertThat(mailer(sender("smtp.example.org"), "").enabled()).isFalse();
        assertThat(mailer(sender("smtp.example.org"), "SevaDesk <no-reply@example.org>").enabled()).isTrue();
    }

    private static JavaMailSenderImpl sender(String host) {
        var s = new JavaMailSenderImpl();
        s.setHost(host);
        return s;
    }

    private static Mailer mailer(JavaMailSender sender, String from) {
        var factory = new DefaultListableBeanFactory();
        if (sender != null) factory.registerSingleton("mail", sender);
        return new Mailer(factory.getBeanProvider(JavaMailSender.class), from, false);
    }
}
