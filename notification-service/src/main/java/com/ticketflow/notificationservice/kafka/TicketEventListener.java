package com.ticketflow.notificationservice.kafka;


import java.time.Instant;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.notificationservice.event.TicketEvent;
import com.ticketflow.notificationservice.model.Notification;
import com.ticketflow.notificationservice.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {
    private final NotificationRepository notificationRepository;
    private final JsonMapper jsonMapper;

    private static final Map<String, String> PRIORITY_FR = Map.of(
            "CRITICAL", "critique", "HIGH", "haute", "MEDIUM", "moyenne", "LOW", "basse");

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
        // String text = switch (event.type()) {
        //     case "TICKET_CREATED" -> "Votre ticket #" + event.ticketId() + " \"" + event.title()
        //             + "\" a été créé (priorité " + event.priority() + ").";
        //     case "STATUS_CHANGED" -> "Votre ticket #" + event.ticketId() + " est maintenant " + event.status() + ".";
        //     case "PRIORITY_CHANGED" -> "La priorité de votre ticket #" + event.ticketId()
        //             + " a été requalifiée : " + event.priority() + ".";
        //     case "TICKET_TRIAGED" -> "Votre ticket #" + event.ticketId()
        //             + " a été qualifié par notre équipe : priorité " + event.priority() + ".";
        //     default -> "Mise à jour du ticket #" + event.ticketId();
        // };
        String text = buildMessage(event);

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


        /** Message clair pour le client (plus de valeurs techniques comme ESCALATED). */
    private String buildMessage(TicketEvent e) {
        String ticket = "Votre ticket #" + e.ticketId();
        String priority = PRIORITY_FR.getOrDefault(e.priority(), e.priority());

        return switch (e.type()) {
            case "TICKET_CREATED"   -> ticket + " « " + e.title() + " » a été créé (priorité " + priority + ").";
            case "TICKET_TRIAGED"   -> ticket + " a été qualifié par notre équipe : priorité " + priority + ".";
            case "PRIORITY_CHANGED" -> "La priorité de votre ticket #" + e.ticketId() + " a été ajustée : " + priority + ".";
            case "STATUS_CHANGED"   -> switch (e.status()) {
                case "ASSIGNED"  -> ticket + " a été pris en charge par un conseiller.";
                case "ESCALATED" -> ticket + " a été transmis à un responsable : le délai prévu est dépassé.";
                case "CLOSED"    -> ticket + " a été résolu.";
                case "REJECTED"  -> ticket + " n'a pas pu être pris en charge : aucun conseiller disponible.";
                default          -> ticket + " a été mis à jour.";
            };
            default -> ticket + " a été mis à jour.";
        };
    }



}
