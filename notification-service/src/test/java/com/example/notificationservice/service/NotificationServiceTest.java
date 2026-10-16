package com.example.notificationservice.service;

import com.example.common.dto.UserEvent;
import com.example.common.dto.UserEventType;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.UserEventNotificationRequest;
import com.example.notificationservice.eventprocessing.UserEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private UserEventRepository userEventRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("sendNotification: отправляет письмо с заданными subject и text")
    void sendNotification_subjectAndTextAreExists() {
        NotificationRequest request = new NotificationRequest(
                "ivan@example.com", "Тема", "Текст");

        notificationService.sendNotification(request);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("Тема", message.getSubject());
        assertEquals("Текст", message.getText());
    }

    @Test
    @DisplayName("sendNotification: отправляет письмо с пустыми subject и text")
    void sendNotification_subjectAndTextAreNull() {
        NotificationRequest request = new NotificationRequest(
                "ivan@example.com", null, null);

        notificationService.sendNotification(request);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertThat(message.getSubject()).isEmpty();
        assertThat(message.getText()).isEmpty();
    }

    @Test
    @DisplayName("sendUserEventNotification: отправляет письмо о создании аккаунта")
    void sendUserEventNotification_createUser() {
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com", UserEventType.CREATE_USER);

        notificationService.sendUserEventNotification(request);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - создание пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно создан.", message.getText());
    }

    @Test
    @DisplayName("sendUserEventNotification: отправляет письмо об удалении аккаунта")
    void sendUserEventNotification_deleteUser() {
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com", UserEventType.DELETE_USER);

        notificationService.sendUserEventNotification(request);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - удаление пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно удалён.", message.getText());
    }

    @Test
    @DisplayName("processUserEvent — сохранение события в БД и отправка письма о создании аккаунта ")
    void processUserEvent_newEvent_createUser() {
        UUID eventId = UUID.randomUUID();
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.CREATE_USER);

        when(userEventRepository.existsById(eventId)).thenReturn(false);

        notificationService.processUserEvent(newEvent);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - создание пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно создан.", message.getText());
    }

    @Test
    @DisplayName("processUserEvent — сохранение события в БД и отправка письма об удалении аккаунта")
    void processUserEvent_newEvent_deleteUser() {
        UUID eventId = UUID.randomUUID();
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.DELETE_USER);

        when(userEventRepository.existsById(eventId)).thenReturn(false);

        notificationService.processUserEvent(newEvent);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - удаление пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно удалён.", message.getText());
    }

    @Test
    @DisplayName("processUserEvent: событие уже существует в БД, сообщение на email не отправляется")
    void processUserEvent_duplicateEvent() {
        UUID eventId = UUID.fromString("0ab7e264-9012-47d5-8250-118e58fd5f77");
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.DELETE_USER);

        when(userEventRepository.existsById(eventId)).thenReturn(true);

        notificationService.processUserEvent(newEvent);
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}