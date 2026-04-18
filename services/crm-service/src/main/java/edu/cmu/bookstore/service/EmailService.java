package edu.cmu.bookstore.service;

import edu.cmu.bookstore.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.andrew.id:rdhurand}")
    private String andrewId;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(Customer customer) {
        logger.info("Sending welcome email to customer: {}", customer.getUserId());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(customer.getUserId());
        message.setSubject("Activate your book store account");

        String body = String.format(
                "Dear %s,%n" +
                "Welcome to the Book store created by %s.%n" +
                "Exceptionally this time we won’t ask you to click a link to activate your account.",
                customer.getName(),
                andrewId
        );

        message.setText(body);
        mailSender.send(message);

        logger.info("Welcome email successfully sent to {}", customer.getUserId());
    }
}