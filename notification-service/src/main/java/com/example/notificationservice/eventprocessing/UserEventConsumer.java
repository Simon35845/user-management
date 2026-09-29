package com.example.notificationservice.eventprocessing;

import com.example.common.dto.UserEvent;
import com.example.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventConsumer {

    private static final String TOPIC = "notifications";
    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);
    private final NotificationService notificationService;

    public UserEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = TOPIC)
    public void consume(UserEvent event) {
        log.info("Событие принято из Kafka: key={}, eventId={}, eventType={}",
                event.email(), event.eventId(), event.eventType());
        notificationService.processUserEvent(event);
    }
}
