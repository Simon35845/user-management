package com.example.notificationservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Ответ с ошибкой валидации")
public record ValidationErrorResponse(
        @Schema(description = "Общее сообщение об ошибке", example = "Введены некорректные данные")
        String message,
        @Schema(description = "Карта ошибок по полям", example = "{\"email\": \"Введите корректный email\"}")
        Map<String, String> errorMap
) {

    public ValidationErrorResponse(Map<String, String> errorMap) {
        this("Введены некорректные данные", errorMap);
    }
}
