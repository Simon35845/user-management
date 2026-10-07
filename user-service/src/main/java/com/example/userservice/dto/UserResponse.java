package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Класс, который уходит в контроллер
 */
public record UserResponse(
        @Schema(description = "ID пользователя", example = "1")
        Integer id,
        @Schema(description = "Имя пользователя", example = "Иван")
        String name,
        @Schema(description = "Email пользователя", example = "ivan@example.com")
        String email,
        @Schema(description = "Возраст пользователя", example = "25")
        Integer age
) {
    @Override
    public String toString() {
        return String.format("User{id=%d, name=%s, email=%s, age=%d}", id, name, email, age);
    }
}
