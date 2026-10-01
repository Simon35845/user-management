package com.example.userservice.dto;

/**
 * Класс, который уходит в контроллер
 */
public record UserResponse(
        Integer id,
        String name,
        String email,
        Integer age
) {
    @Override
    public String toString() {
        return String.format("User{id=%d, name=%s, email=%s, age=%d}", id, name, email, age);
    }
}
