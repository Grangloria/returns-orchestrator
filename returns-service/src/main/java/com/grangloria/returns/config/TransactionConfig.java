package com.grangloria.returns.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DelegatingTransactionDefinition;

@Configuration
public class TransactionConfig {

    @Bean
    public ReactiveTransactionManager transactionManager(ConnectionFactory connectionFactory) {
        return new R2dbcTransactionManager(connectionFactory) {
            @Override
            protected io.r2dbc.spi.TransactionDefinition createTransactionDefinition(TransactionDefinition definition) {
                String originalName = definition.getName();
                String sanitizedName = null;

                if (originalName != null) {
                    // Strip non-alphanumeric characters (dots, underscores)
                    String sanitized = originalName.replaceAll("[^a-zA-Z0-9]", "");
                    // Truncate to max 32 characters for MSSQL R2DBC compliance
                    sanitizedName = sanitized.length() > 32
                            ? sanitized.substring(sanitized.length() - 32)
                            : sanitized;
                }

                final String finalName = sanitizedName;
                TransactionDefinition sanitizedSpringDefinition = new DelegatingTransactionDefinition(definition) {
                    @Override
                    public String getName() {
                        return finalName;
                    }
                };

                return super.createTransactionDefinition(sanitizedSpringDefinition);
            }
        };
    }
}