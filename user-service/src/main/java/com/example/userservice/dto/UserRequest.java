package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * Класс, который приходит из контроллера
 */
public record UserRequest(
        @Schema(description = "Имя пользователя", example = "Иван", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Имя обязательно")
        @Pattern(regexp = "^[A-Za-zА-Яа-яЁё\\s-]{2,100}$",
                message = "Имя должно содержать только буквы, пробелы и дефис, от 2 до 100 символов")
        String name,

        @Schema(description = "Email пользователя", example = "ivan@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email обязателен")
        @Email(regexp = ".+@.+\\..+", message = "Введите корректный email, например user@example.com")
        String email,

        @Schema(description = "Возраст пользователя", example = "25", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Возраст обязателен")
        @Min(value = 0, message = "Возраст должен быть больше 0")
        @Max(value = 150, message = "Возраст не может быть больше 150 лет")
        Integer age
) {}
