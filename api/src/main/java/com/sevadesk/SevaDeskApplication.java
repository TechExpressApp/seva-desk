package com.sevadesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SevaDesk: a white-label administration platform for temples and places of worship.
 * <p>
 * Each top-level package is a Spring Modulith application module. Modules talk to each other
 * through published events and their top-level (public) types only; see {@code ModularityTests}.
 */
@SpringBootApplication
@EnableScheduling
public class SevaDeskApplication {
    public static void main(String[] args) {
        SpringApplication.run(SevaDeskApplication.class, args);
    }
}
