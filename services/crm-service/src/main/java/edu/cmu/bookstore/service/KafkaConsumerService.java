package edu.cmu.bookstore.service;

import edu.cmu.bookstore.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service responsible for consuming customer registration events from Kafka.
 *
 * When a "Customer Registered" message is received, it triggers the
 * welcome email flow through the {@link EmailService}.
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private final EmailService emailService;

    /**
     * Constructs the consumer service with its required email logic dependency.
     *
     * @param emailService service used to send welcome emails
     */
    public KafkaConsumerService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Listener method that processes messages from the customer event topic.
     *
     * The method uses Spring Kafka's @KafkaListener to automatically
     * deserialize incoming JSON messages into {@link Customer} objects.
     *
     * @param customer customer details received in the Kafka message
     */
    @KafkaListener(topics = "${app.kafka.topic:rdhurand.customer.evt}", groupId = "crm-group")
    public void listenCustomerRegistered(Customer customer) {
        logger.info("Received customer registration event for: {}", customer.getUserId());
        try {
            emailService.sendWelcomeEmail(customer);
        } catch (Exception e) {
            logger.error("Error processing customer registration event", e);
        }
    }
}
