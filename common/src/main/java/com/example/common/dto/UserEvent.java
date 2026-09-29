package com.example.common.dto;

import java.util.UUID;

public record UserEvent(
        UUID eventId,
        String email,
        UserEventType eventType
) {
}
