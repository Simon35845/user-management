package com.example.notificationservice.eventprocessing;

import com.example.common.dto.UserEvent;
import com.example.common.dto.UserEventType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@Transactional
@Testcontainers
@Sql(
        scripts = "/insert-user-events.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class UserEventConsumerIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @MockitoBean
    private JavaMailSender javaMailSender;

    @Autowired
    private UserEventConsumer userEventConsumer;

    @Autowired
    private UserEventRepository userEventRepository;

    @Test
    @DisplayName("consume — сохранение события в БД и отправка письма о создании аккаунта ")
    void consume_newEvent_createUser() {
        UUID eventId = UUID.randomUUID();
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.CREATE_USER);
        userEventConsumer.consume(newEvent);

        assertTrue(userEventRepository.existsById(eventId));
        assertEquals(3, userEventRepository.count());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - создание пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно создан.", message.getText());
    }

    @Test
    @DisplayName("consume — сохранение события в БД и отправка письма об удалении аккаунта")
    void consume_newEvent_deleteUser() {
        UUID eventId = UUID.randomUUID();
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.DELETE_USER);
        userEventConsumer.consume(newEvent);

        assertTrue(userEventRepository.existsById(eventId));
        assertEquals(3, userEventRepository.count());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - удаление пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно удалён.", message.getText());
    }

    @Test
    @DisplayName("consume — событие уже существует в БД, сообщение на email не отправляется")
    void consume_duplicateEvent() {
        UUID eventId = UUID.fromString("0ab7e264-9012-47d5-8250-118e58fd5f77");
        UserEvent newEvent = new UserEvent(eventId, "ivan@example.com", UserEventType.DELETE_USER);
        userEventConsumer.consume(newEvent);

        assertTrue(userEventRepository.existsById(eventId));
        assertEquals(2, userEventRepository.count());
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}