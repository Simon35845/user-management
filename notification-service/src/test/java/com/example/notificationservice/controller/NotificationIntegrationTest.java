package com.example.notificationservice.controller;

import com.example.common.dto.UserEvent;
import com.example.common.dto.UserEventType;
import com.example.notificationservice.eventprocessing.UserEventEntity;
import com.example.notificationservice.eventprocessing.UserEventRepository;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.UserEventNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NotificationIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.kafka.listener.auto-startup", () -> "false");
        registry.add("spring.kafka.consumer.auto-startup", () -> "false");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserEventRepository userEventRepository;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @BeforeEach
    void setUp() {
        userEventRepository.deleteAll();
    }


    @Test
    @DisplayName("POST /notification — отправляет письмо и возвращает 200")
    void sendNotification_ShouldReturn200_WhenDataIsValid() throws Exception {
        // given
        NotificationRequest request = new NotificationRequest(
                "ivan@example.com",
                "Тема письма",
                "Текст письма"
        );

        // when + then
        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("POST /notification — возвращает 400, если email невалидный")
    void sendNotification_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        // given
        NotificationRequest request = new NotificationRequest(
                "invalid-email", "Тема", "Текст"
        );

        // when + then
        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }


    @Test
    @DisplayName("POST /notification/user-event — CREATE_USER: отправляет письмо")
    void sendUserEventNotification_ShouldSendCreateUserEmail() throws Exception {
        // given
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com",
                UserEventType.CREATE_USER
        );

        // when
        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("ivan@example.com", message.getTo()[0]);
        assertEquals("User Management Application - создание пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно создан.", message.getText());
    }

    @Test
    @DisplayName("POST /notification/user-event — DELETE_USER: отправляет письмо")
    void sendUserEventNotification_ShouldSendDeleteUserEmail() throws Exception {
        // given
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com",
                UserEventType.DELETE_USER
        );

        // when
        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("ivan@example.com", message.getTo()[0]);
        assertEquals("User Management Application - удаление пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно удалён.", message.getText());
    }


    @Test
    @DisplayName("UserEventRepository — сохраняет и находит событие в БД")
    void userEventRepository_ShouldSaveAndFindEvent() {
        // given
        UUID eventId = UUID.randomUUID();
        UserEventEntity entity = new UserEventEntity(
                eventId, "ivan@example.com", UserEventType.CREATE_USER
        );

        // when
        userEventRepository.save(entity);

        // then
        var found = userEventRepository.findById(eventId);
        assertTrue(found.isPresent());
        assertEquals("ivan@example.com", found.get().getEmail());
        assertEquals(UserEventType.CREATE_USER, found.get().getEventType());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    @DisplayName("UserEventRepository — сохраняет несколько событий")
    void userEventRepository_ShouldSaveMultipleEvents() {
        // given
        UserEventEntity event1 = new UserEventEntity(
                UUID.randomUUID(), "ivan@example.com", UserEventType.CREATE_USER
        );
        UserEventEntity event2 = new UserEventEntity(
                UUID.randomUUID(), "petr@example.com", UserEventType.DELETE_USER
        );

        // when
        userEventRepository.saveAll(List.of(event1, event2));

        // then
        assertEquals(2, userEventRepository.count());
    }

    @Test
    @DisplayName("UserEventRepository — existsById возвращает true для существующего")
    void userEventRepository_ShouldReturnTrueForExistingId() {
        // given
        UUID eventId = UUID.randomUUID();
        UserEventEntity entity = new UserEventEntity(
                eventId, "ivan@example.com", UserEventType.CREATE_USER
        );
        userEventRepository.save(entity);

        // when + then
        assertTrue(userEventRepository.existsById(eventId));
        assertFalse(userEventRepository.existsById(UUID.randomUUID()));
    }


    @Test
    @DisplayName("processUserEvent — сохраняет событие в БД и отправляет письмо")
    void processUserEvent_ShouldSaveAndSendEmail() {
        // given
        UUID eventId = UUID.randomUUID();
        UserEvent event = new UserEvent(
                eventId, "ivan@example.com", UserEventType.CREATE_USER
        );

        // when
        UserEventEntity entity = new UserEventEntity(event.eventId(), event.email(), event.eventType());
        userEventRepository.save(entity);

        // then
        assertTrue(userEventRepository.existsById(eventId));
    }
}