package com.ticketflow.workflowservice.kafka;

import com.ticketflow.workflowservice.event.TicketEvent;
import io.camunda.client.CamundaClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @KafkaListener(topics = "ticket-events")
    public void onTicketEvent(String message) {
        TicketEvent event = jsonMapper.readValue(message, TicketEvent.class);
        if (!"TICKET_CREATED".equals(event.type())) return;   // seulement les créations

        var instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("ticket-lifecycle")
                .latestVersion()
                .variables(Map.of(
                        "ticketId", event.ticketId(),
                        "clientId", event.clientId(),
                        "title", event.title()))
                .send()
                .join();

        log.info("Process started for ticket {} (instance {})",
                event.ticketId(), instance.getProcessInstanceKey());
    }
}