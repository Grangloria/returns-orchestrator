package com.grangloria.notification.service;

import com.grangloria.notification.event.ReturnLabelReadyEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService
{

    private final JavaMailSender mailSender;

    public void sendReturnReadyLabelEmail(ReturnLabelReadyEvent event) {
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

        log.info("[EMAIL-SERVICE] Confirmation return label ready email dispatched successfully to [{}]", event.customerEmail());
    }
}