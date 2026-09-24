package com.example.notificationservice.dto;

import java.util.Map;

public record ValidationErrorResponse(
        String message,
        Map<String, String> errorMap
) {

    public ValidationErrorResponse(Map<String, String> errorMap) {
        this("Введены некорректные данные", errorMap);
    }
}
