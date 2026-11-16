package com.ticketflow.workflowservice.kafka;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.ticketflow.workflowservice.config.SlaProperties;
import com.ticketflow.workflowservice.event.TicketEvent;

import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private final CamundaClient camundaClient;
    private final JsonMapper jsonMapper;
    private final SlaProperties slaProperties;

    @KafkaListener(topics = "ticket-events")
    public void onEvent(String message) {
        TicketEvent event = jsonMapper.readValue(message, TicketEvent.class);

        switch (event.type()) {
            case "TICKET_CREATED" -> startProcess(event);
            case "STATUS_CHANGED" -> {
                if ("CLOSED".equals(event.status())) resolve(event);
            }
            case "PRIORITY_CHANGED" -> requalify(event);
            default -> { }   // on ignore les autres événements (dont TICKET_TRIAGED)
        }
    }

    private void startProcess(TicketEvent event) {
        String priority = event.priority() != null ? event.priority() : "MEDIUM";  // anciens events
        String slaDuration = slaProperties.sla()
                .getOrDefault(priority, Duration.ofMinutes(5))
                .toString();                                                       // ex. "PT1M"

        // HashMap (et pas Map.of) : Map.of refuse les valeurs null
        Map<String, Object> variables = new HashMap<>();
        variables.put("ticketId", event.ticketId());
        variables.put("clientId", event.clientId());
        variables.put("title", event.title());
        variables.put("description", event.description() != null ? event.description() : "");  // lu par l'agent de tri
        variables.put("priority", priority);
        variables.put("slaDuration", slaDuration);
        variables.put("needsReview", Boolean.TRUE.equals(event.needsReview()));                 // lu par le BPMN

                // Demarrage par MESSAGE : une seule instance active par ticket (correlationKey),
        // TTL 0 : un doublon recu pendant que l'instance tourne est simplement ignore.
        camundaClient.newPublishMessageCommand()
                .messageName("ticket-created")
                .correlationKey(String.valueOf(event.ticketId()))
                .messageId(event.eventId())
                .timeToLive(Duration.ZERO)
                .variables(variables)
                .send()
                .join();

        log.info("Process requested for ticket {} (priority {}, SLA {}, needsReview {})",
                event.ticketId(), priority, slaDuration, event.needsReview());
    }

    private void resolve(TicketEvent event) {
        camundaClient.newPublishMessageCommand()
                .messageName("ticket-resolved")
                .correlationKey(String.valueOf(event.ticketId()))
                .send()
                .join();

        log.info("Message ticket-resolved sent for ticket {}", event.ticketId());
    }

    private void requalify(TicketEvent event) {
        String slaDuration = slaProperties.sla()
                .getOrDefault(event.priority(), Duration.ofMinutes(5))
                .toString();

        camundaClient.newPublishMessageCommand()
                .messageName("priority-changed")
                .correlationKey(String.valueOf(event.ticketId()))
                .variables(Map.of(
                        "priority", event.priority(),
                        "slaDuration", slaDuration))     // le nouveau SLA arrive AVEC le message
                .send()
                .join();

        log.info("Ticket {} requalified to {} (new SLA {})", event.ticketId(), event.priority(), slaDuration);
    }
}