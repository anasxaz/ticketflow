package com.ticketflow.notificationservice.kafka;


import com.ticketflow.notificationservice.event.TicketEvent;
import com.ticketflow.notificationservice.model.Notification;
import com.ticketflow.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {
    private final NotificationRepository notificationRepository;
    private final JsonMapper jsonMapper;

    @KafkaListener(topics = "ticket-events")
    @Transactional
    public void onTicketEvent(String message){
        TicketEvent event = jsonMapper.readValue(message, TicketEvent.class);

        // 1. Idempotence : déjà traité ?
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.warn("Duplicate event ignored: {}", event.eventId());
            return;
        }

        // 2. Construire le message
        String text = switch (event.type()) {
            case "TICKET_CREATED" -> "Votre ticket #" + event.ticketId() + " \"" + event.title() + "\" a été créé.";
            case "STATUS_CHANGED" -> "Votre ticket #" + event.ticketId() + " est maintenant " + event.status() + ".";
            default -> "Mise à jour du ticket #" + event.ticketId();
        };

        // 3. Sauvegarder + "envoyer"
        notificationRepository.save(Notification.builder()
                .eventId(event.eventId())
                .ticketId(event.ticketId())
                .clientId(event.clientId())
                .message(text)
                .createdAt(Instant.now())
                .build());

        log.info("Notification to client {}: {}", event.clientId(), text);
    }



}
