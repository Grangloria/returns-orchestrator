package com.grangloria.notification;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class NotificationApplication {

    @PostConstruct
    public void init() {
        // Tells Project Reactor to automatically propagate
        // Micrometer tracing contexts across non-blocking thread transitions.
        Hooks.enableAutomaticContextPropagation();
    }

    public static void main(String[] args) {
        SpringApplication.run(NotificationApplication.class, args);
    }
}