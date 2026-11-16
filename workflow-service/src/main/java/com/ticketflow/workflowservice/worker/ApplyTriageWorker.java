package com.ticketflow.workflowservice.worker;

import com.ticketflow.workflowservice.client.TicketClient;
import com.ticketflow.workflowservice.config.SlaProperties;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplyTriageWorker {

    private final TicketClient ticketClient;
    private final SlaProperties slaProperties;

    /** Les variables category et blocked viennent du FORMULAIRE rempli par l'agent de tri. */
    @JobWorker(type = "apply-triage")
    public Map<String, Object> applyTriage(@Variable Long ticketId,
                                           @Variable String category,
                                           @Variable Boolean blocked) {
        Map<String, Object> ticket = ticketClient.triage(ticketId,
                Map.of("category", category, "blocked", Boolean.TRUE.equals(blocked)));

        String priority = (String) ticket.get("priority");                  // calculee par les REGLES
        String slaDuration = slaProperties.sla()
                .getOrDefault(priority, Duration.ofMinutes(5)).toString();

        log.info("Ticket {} trie par un humain : {} -> SLA {}", ticketId, priority, slaDuration);
        return Map.of("priority", priority, "slaDuration", slaDuration, "needsReview", false);
    }
}