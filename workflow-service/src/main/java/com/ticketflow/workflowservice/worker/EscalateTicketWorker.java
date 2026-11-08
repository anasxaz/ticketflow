package com.ticketflow.workflowservice.worker;

import com.ticketflow.workflowservice.client.TicketClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EscalateTicketWorker {

    private final TicketClient ticketClient;

    @JobWorker(type = "escalate-ticket")
    public void escalate(@Variable Long ticketId) {
        ticketClient.updateStatus(ticketId, Map.of("status", "ESCALATED"));
        log.warn("SLA exceeded → ticket {} ESCALATED", ticketId);
    }
}