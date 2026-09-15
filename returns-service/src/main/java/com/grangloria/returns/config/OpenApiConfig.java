package com.grangloria.returns.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Returns Orchestrator API")
                        .version("1.0.0")
                        .description("A high-concurrency Reactive microservice for managing product returns.")
                        .contact(new Contact()
                                .name("James Hayes")
                                .email("james.hayes2@outlook.com")));
    }
}