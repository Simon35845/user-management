package com.example.notificationservice.service;

import com.example.common.dto.UserEvent;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.UserEventNotificationRequest;
import com.example.notificationservice.eventprocessing.UserEventEntity;
import com.example.notificationservice.eventprocessing.UserEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final JavaMailSender javaMailSender;
    private final UserEventRepository userEventRepository;

    public NotificationService(JavaMailSender javaMailSender, UserEventRepository userEventRepository) {
        this.javaMailSender = javaMailSender;
        this.userEventRepository = userEventRepository;
    }

    public void sendNotification(NotificationRequest request) {
        log.info("Отправка сообщения: email={}, subject={}", request.email(), request.subject());
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.email());
        message.setSubject(Objects.toString(request.subject(), ""));
        message.setText(Objects.toString(request.text(), ""));
        javaMailSender.send(message);
    }

    public void sendUserEventNotification(UserEventNotificationRequest request) {
        log.info("Отправка сообщения с пользовательским событием: email={}, eventType={}", request.email(), request.eventType());

        String subject = "";
        String text = "";

        switch (request.eventType()) {
            case CREATE_USER -> {
                subject = "User Management Application - создание пользователя";
                text = "Здравствуйте! Ваш аккаунт был успешно создан.";
            }
            case DELETE_USER -> {
                subject = "User Management Application - удаление пользователя";
                text = "Здравствуйте! Ваш аккаунт был успешно удалён.";
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(request.email());
        message.setSubject(subject);
        message.setText(text);
        javaMailSender.send(message);
    }

    @Transactional
    public void processUserEvent(UserEvent event) {
        log.info("Обработка события: eventId={}, email={}, eventType={}",
                event.eventId(), event.email(), event.eventType());

        if (!userEventRepository.existsById(event.eventId())) {
            UserEventEntity userEventEntity = new UserEventEntity(
                    event.eventId(),
                    event.email(),
                    event.eventType()
            );
            userEventRepository.save(userEventEntity);
            log.info("Событие сохранено в БД: eventId={}, email={}, eventType={}",
                    event.eventId(), event.email(), event.eventType());

            UserEventNotificationRequest request = new UserEventNotificationRequest(event.email(), event.eventType());
            sendUserEventNotification(request);
        } else {
            log.warn("Такое событие уже есть в БД: eventId={}, email={}, eventType={}",
                    event.eventId(), event.email(), event.eventType());
        }
    }
}
