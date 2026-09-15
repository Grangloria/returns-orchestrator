package com.grangloria.returns;

import com.grangloria.returns.config.KafkaTopicProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class ReturnsApplication {

    @PostConstruct
    public void init() {
        // Tells Project Reactor to automatically propagate
        // Micrometer tracing contexts across non-blocking thread transitions.
        Hooks.enableAutomaticContextPropagation();
    }

    public static void main(String[] args) {
        // This is the command that starts the Netty server (WebFlux)
        // and wires up all the @Service and @RestController classes.
        SpringApplication.run(ReturnsApplication.class, args);
    }
}