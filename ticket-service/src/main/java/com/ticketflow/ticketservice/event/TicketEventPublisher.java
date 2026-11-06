package com.ticketflow.ticketservice.event;

import com.ticketflow.ticketservice.config.KafkaTopicConfig;
import com.ticketflow.ticketservice.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publish(String type, Ticket ticket){
        TicketEvent event = new TicketEvent(
                UUID.randomUUID().toString(),
                type,
                ticket.getId(),
                ticket.getClientId(),
                ticket.getTitle(),
                ticket.getStatus().name(),
                Instant.now()
        );
        String json = jsonMapper.writeValueAsString(event);
        kafkaTemplate.send(KafkaTopicConfig.TICKET_EVENTS, ticket.getId().toString(), json);
        log.info("Event published : {} for ticket {}", type, ticket.getId());
    }
}

