package com.ticketflow.workflowservice.kafka;

import com.ticketflow.workflowservice.event.TicketEvent;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketEventListener {

    private final CamundaClient camundaClient;
    private final JsonMapper jsonMapper;

    @Value("${ticketflow.sla-duration:PT2M}")
    private String slaDuration;

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
        var instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("ticket-lifecycle")
                .latestVersion()
                .variables(Map.of(
                        "ticketId", event.ticketId(),
                        "clientId", event.clientId(),
                        "title", event.title(),
                        "slaDuration", slaDuration))
                .send()
                .join();

        log.info("Process started for ticket {} (instance {})",
                event.ticketId(), instance.getProcessInstanceKey());
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