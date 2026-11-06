package com.ticketflow.notificationservice.repository;

import com.ticketflow.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByEventId(String eventId);
}
