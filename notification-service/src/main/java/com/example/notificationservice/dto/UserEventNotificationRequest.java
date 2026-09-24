package com.example.notificationservice.dto;

import com.example.common.dto.UserEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserEventNotificationRequest(

        @Schema(description = "Email пользователя", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "email пользователя обязателен")
        @Email(regexp = ".+@.+\\..+", message = "Введите корректный email, например user@example.com")
        String email,

        @Schema(description = "Тип пользовательского события", requiredMode = Schema.RequiredMode.REQUIRED)
        UserEventType eventType
) {
}
