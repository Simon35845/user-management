package com.example.notificationservice.controller;

import com.example.common.dto.UserEventType;
import com.example.notificationservice.eventprocessing.UserEventConsumer;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.UserEventNotificationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Testcontainers
@Sql(
        scripts = "/insert-user-events.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
class NotificationControllerIntegrationTest {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private UserEventConsumer userEventConsumer;

    @Test
    @DisplayName("POST /notification — отправляет письмо и возвращает 200")
    void sendNotification_ShouldReturn200_WhenDataIsValid() throws Exception {
        NotificationRequest request = new NotificationRequest(
                "ivan@example.com",
                "Тема",
                "Текст"
        );

        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("Тема", message.getSubject());
        assertEquals("Текст", message.getText());
    }

    @Test
    @DisplayName("POST /notification — возвращает 400, если email невалидный")
    void sendNotification_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        NotificationRequest request = new NotificationRequest(
                "invalid-email",
                "Тема",
                "Текст"
        );

        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("POST /notification/user-event — отправляет письмо о создании аккаунта")
    void sendUserEventNotification_ShouldReturn200_CreateUserEvent() throws Exception {
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com",
                UserEventType.CREATE_USER
        );

        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - создание пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно создан.", message.getText());
    }

    @Test
    @DisplayName("POST /notification/user-event — отправляет письмо об удалении аккаунта")
    void sendUserEventNotification_ShouldReturn200_DeleteUserEvent() throws Exception {
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "ivan@example.com",
                UserEventType.DELETE_USER
        );

        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("ivan@example.com");
        assertEquals("User Management Application - удаление пользователя", message.getSubject());
        assertEquals("Здравствуйте! Ваш аккаунт был успешно удалён.", message.getText());
    }

    @Test
    @DisplayName("POST /notification/user-event — возвращает 400, если email невалидный")
    void sendUserEventNotification_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "invalid-email",
                UserEventType.CREATE_USER
        );

        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("POST /notification/user-event — возвращает 400, если eventType невалидный")
    void sendUserEventNotification_ShouldReturn400_WhenEventTypeIsInvalid() throws Exception {
        String requestJson = """
                {
                    "email": "ivan@example.com",
                    "eventType": "UNKNOWN_EVENT"
                }
                """;

        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}