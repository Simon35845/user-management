package com.example.userservice.mapper;

import com.example.userservice.dto.UserRequest;
import com.example.userservice.dto.UserResponse;
import com.example.userservice.entity.UserEntity;

/**
 * Маппинг данных для запросов и выходных данных
 */
public final class UserMapper {
    private UserMapper() {
    }

    public static UserResponse toResponse(UserEntity entity) {
        if (entity == null) return null;
        return new UserResponse(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getAge()
        );
    }

    public static UserEntity toEntity(UserRequest request) {
        return new UserEntity(request.name(), request.email(), request.age());
    }
}
