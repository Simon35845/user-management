package com.example.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(

        @Schema(description = "Email пользователя", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email пользователя обязателен")
        @Email(regexp = ".+@.+\\..+", message = "Введите корректный email, например user@example.com")
        String email,

        @Schema(description = "Тема сообщения", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String subject,

        @Schema(description = "Текст сообщения", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String text
) {
}
