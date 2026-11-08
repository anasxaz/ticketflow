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
public class RejectTicketWorker {

    private final TicketClient ticketClient;

    @JobWorker(type = "reject-ticket")
    public void reject(@Variable Long ticketId) {
        ticketClient.updateStatus(ticketId, Map.of("status", "REJECTED"));
        log.info("No agent available → ticket {} REJECTED (compensation)", ticketId);
    }
}
