package com.example.userservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record PaginationRequest(
        @Min(value = 0, message = "Номер страницы не может быть меньше 0")
        @Schema(description = "Номер страницы", example = "1")
        int page,

        @Min(value = 1, message = "Количество пользователей должно быть больше 0")
        @Schema(description = "Количество пользователей на странице", example = "2")
        int size,

        @Pattern(regexp = "id|name|email|age",
                message = "Недопустимое поле сортировки")
        @Schema(description = "Сортировка (необязательный параметр)", example = "age")
        String sortBy
) { }