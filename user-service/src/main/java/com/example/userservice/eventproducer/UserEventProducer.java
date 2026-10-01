package com.example.userservice.eventproducer;

import com.example.common.dto.UserEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventProducer {

    private static final String TOPIC = "notifications";
    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);
    private final KafkaTemplate<String, UserEvent> kafkaTemplate;

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(UserEvent event) {
        kafkaTemplate.send(TOPIC, event.email(), event);
        log.info("Событие отправлено в Kafka: key={}, eventId={}, eventType={}",
                event.email(), event.eventId(), event.eventType());
    }
}
