package com.ticketflow.notificationservice.repository;

import com.ticketflow.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);
    List<Notification> findByClientIdOrderByCreatedAtDesc(Long clientId);
}
