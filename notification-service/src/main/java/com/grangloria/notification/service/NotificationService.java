package com.grangloria.notification.service;

import com.grangloria.notification.event.ReturnLabelReadyEvent;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final RetryRegistry retryRegistry;

    public void sendReturnReadyLabelEmail(ReturnLabelReadyEvent event) {
        // Retrieve my retry instance from the application.yaml file
        Retry emailRetry = retryRegistry.retry("emailServiceRetry");

        // Decorate the blocking mailSender call with Resilience4j retry mechanics
        Runnable decoratedEmailTask = Retry.decorateRunnable(emailRetry, () -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("james.hayes@grangloria.com");
            message.setTo(event.customerEmail());
            message.setSubject("Return Initiated - Order #" + event.orderId());

            String body = String.format(
                    "Hello,\n\n" +
                            "Your return label for Order ID %s has been successfully generated.\n" +
                            "Label URL: %s\n\n" +
                            "Thank you for shopping with Grangloria!",
                    event.orderId(), event.labelUrl()
            );

            message.setText(body);

            log.info("[EMAIL-SERVICE] Dispatching return label ready email to [{}] for Order ID: [{}]",
                    event.customerEmail(), event.orderId());

            mailSender.send(message);
        });

        try {
            // Execute decorated task (handles initial execution + exponential backoff retries)
            decoratedEmailTask.run();
            log.info("[EMAIL-SERVICE] Confirmation return label ready email dispatched successfully to [{}]", event.customerEmail());
        } catch (Exception e) {
            log.error("[EMAIL-SERVICE] Retries exhausted. Failed to send return label email to [{}] for Order ID: [{}]. Error: {}",
                    event.customerEmail(), event.orderId(), e.getMessage());

            // Re-throw exception so the calling Kafka listener triggers its error handler / DLT workflow
            throw e;
        }
    }
}