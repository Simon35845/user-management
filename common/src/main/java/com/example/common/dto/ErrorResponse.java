package com.example.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Стандартный ответ с информацией об ошибке")
public record ErrorResponse(

        @Schema(
                description = "Общее сообщение об ошибке",
                example = "Введены некорректные данные",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,

        @Schema(
                description = "Время возникновения ошибки (UTC)",
                example = "2026-10-07T12:34:56.789Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp,

        @Schema(
                description = "Детали ошибки по полям (только для ошибок валидации)",
                example = "{\"email\": \"Введите корректный email\", \"age\": \"Возраст должен быть больше 0\"}",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Map<String, String> details
) {}