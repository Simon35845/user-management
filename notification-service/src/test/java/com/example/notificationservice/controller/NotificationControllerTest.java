package com.example.notificationservice.controller;

import com.example.common.dto.UserEventType;
import com.example.notificationservice.dto.NotificationRequest;
import com.example.notificationservice.dto.UserEventNotificationRequest;
import com.example.notificationservice.eventprocessing.UserEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        KafkaAutoConfiguration.class
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JavaMailSender javaMailSender;

    @MockitoBean
    private UserEventRepository userEventRepository;

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
    @DisplayName("POST /notification — отправляет письмо с правильными полями")
    void sendNotification_ShouldSendCorrectMessage() throws Exception {
        // given
        NotificationRequest request = new NotificationRequest(
                "ivan@example.com",
                "Тема письма",
                "Текст письма"
        );

        // when
        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("ivan@example.com", message.getTo()[0]);
        assertEquals("Тема письма", message.getSubject());
        assertEquals("Текст письма", message.getText());
    }

    @Test
    @DisplayName("POST /notification — возвращает 400, если email невалидный")
    void sendNotification_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        // given
        NotificationRequest request = new NotificationRequest(
                "invalid-email",
                "Тема",
                "Текст"
        );

        // when + then
        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("POST /notification — возвращает 400, если email пустой")
    void sendNotification_ShouldReturn400_WhenEmailIsBlank() throws Exception {
        // given
        NotificationRequest request = new NotificationRequest(
                "",
                "Тема",
                "Текст"
        );

        // when + then
        mockMvc.perform(post("/notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }


    @Test
    @DisplayName("POST /notification/user-event — CREATE_USER: отправляет письмо с правильным subject/text")
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
    @DisplayName("POST /notification/user-event — DELETE_USER: отправляет письмо с правильным subject/text")
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
    @DisplayName("POST /notification/user-event — возвращает 400, если email невалидный")
    void sendUserEventNotification_ShouldReturn400_WhenEmailIsInvalid() throws Exception {
        // given
        UserEventNotificationRequest request = new UserEventNotificationRequest(
                "invalid-email",
                UserEventType.CREATE_USER
        );

        // when + then
        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("POST /notification/user-event — возвращает 400, если eventType невалидный")
    void sendUserEventNotification_ShouldReturn400_WhenEventTypeIsInvalid() throws Exception {
        // given — eventType не существует
        String requestJson = """
                {
                    "email": "ivan@example.com",
                    "eventType": "UNKNOWN_EVENT"
                }
                """;

        // when + then
        mockMvc.perform(post("/notification/user-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());

        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }
}