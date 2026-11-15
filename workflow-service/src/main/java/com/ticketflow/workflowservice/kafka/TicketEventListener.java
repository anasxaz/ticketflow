package com.ticketflow.workflowservice.kafka;

import com.ticketflow.workflowservice.config.SlaProperties;
import com.ticketflow.workflowservice.event.TicketEvent;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.time.Duration;

import java.util.Map;

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
            default -> { }   // on ignore les autres événements
        }
    }

    private void startProcess(TicketEvent event) {
        String priority = event.priority() != null ? event.priority() : "MEDIUM";  // anciens events
        String slaDuration = slaProperties.sla()
                .getOrDefault(priority, Duration.ofMinutes(5))
                .toString();                                                       // ex. "PT1M"

        var instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("ticket-lifecycle")
                .latestVersion()
                .variables(Map.of(
                        "ticketId", event.ticketId(),
                        "clientId", event.clientId(),
                        "title", event.title(),
                        "priority", priority,          // visible dans Operate
                        "slaDuration", slaDuration))
                .send()
                .join();

        log.info("Process started for ticket {} (priority {}, SLA {}, instance {})",
                event.ticketId(), priority, slaDuration, instance.getProcessInstanceKey());
    }

    private void resolve(TicketEvent event) {
        camundaClient.newPublishMessageCommand()
                .messageName("ticket-resolved")
                .correlationKey(String.valueOf(event.ticketId()))
                .send()
                .join();

        log.info("Message ticket-resolved sent for ticket {}", event.ticketId());
    }
}