package com.example.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record NotificationRequest(
        @Schema(description = "Email получателя", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email получателя обязателен")
        @Email(regexp = ".+@.+\\..+", message = "Введите корректный email, например user@example.com")
        String to,

        @Schema(description = "Тема сообщения", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String subject,

        @Schema(description = "Текст сообщения", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String text
) {
}
