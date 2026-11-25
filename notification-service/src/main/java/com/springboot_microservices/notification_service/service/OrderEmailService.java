package com.springboot_microservices.notification_service.service;

import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.springboot_microservices.messaging.OrderCreatedEvent;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean emailEnabled;

    public OrderEmailService(
            JavaMailSender mailSender,
            @Value("${NOTIFICATION_EMAIL_FROM:no-reply@localhost}") String fromAddress,
            @Value("${NOTIFICATION_EMAIL_ENABLED:false}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.emailEnabled = emailEnabled;
    }

    public void sendOrderConfirmation(OrderCreatedEvent event) {
        String items = event.items().stream()
                .map(item -> item.skuCode() + " x " + item.quantity())
                .collect(Collectors.joining("\n"));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.customerEmail());
        message.setSubject("Confirmación del pedido #" + event.orderId());
        message.setText("Hemos recibido tu pedido #" + event.orderId()
                + ".\n\nArtículos:\n" + items
                + "\n\nGracias por tu compra.");

        if (!emailEnabled) {
            log.info("Simulación de correo para el pedido {} (evento {}). No se envió por SMTP.",
                    event.orderId(), event.eventId());
            return;
        }

        mailSender.send(message);
    }
}
