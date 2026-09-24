package com.example.notificationservice.eventprocessing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserEventRepository extends JpaRepository<UserEventEntity, UUID> {
}
