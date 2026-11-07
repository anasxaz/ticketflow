package com.ticketflow.notificationservice.controller;

import com.ticketflow.notificationservice.client.UserClient;
import com.ticketflow.notificationservice.model.Notification;
import com.ticketflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationRepository notificationRepository;
    private final UserClient userClient;

//    @GetMapping
//    public List<Notification> getAll(){
//        return notificationRepository.findAll();
//    }


    @GetMapping
    public List<Notification> getMine(@RequestHeader("X-User-Email") String email,
                                      @RequestHeader("X-User-Roles") String roles) {
        if (roles.contains("ROLE_AGENT")) {
            return notificationRepository.findAll();          // un agent voit tout
        }
        Long clientId = userClient.getUserByEmail(email).id();
        return notificationRepository.findByClientIdOrderByCreatedAtDesc(clientId);
    }
}
