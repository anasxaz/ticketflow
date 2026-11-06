package com.ticketflow.notificationservice.controller;

import com.ticketflow.notificationservice.model.Notification;
import com.ticketflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;

    @GetMapping
    public List<Notification> getAll(){
        return notificationRepository.findAll();
    }
}
