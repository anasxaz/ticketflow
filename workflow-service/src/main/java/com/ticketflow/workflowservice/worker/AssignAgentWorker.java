package com.ticketflow.workflowservice.worker;

import com.ticketflow.workflowservice.client.TicketClient;
import com.ticketflow.workflowservice.client.UserClient;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.exception.CamundaError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Component
@RequiredArgsConstructor
public class AssignAgentWorker {

    private final UserClient userClient;
    private final TicketClient ticketClient;

    @JobWorker(type = "assign-agent")
    public Map<String, Object> assignAgent(@Variable Long ticketId) {
        List<UserClient.UserResponse> agents = userClient.getAgents();
        //if (agents.isEmpty()) throw new IllegalStateException("No agent available");
        if (agents.isEmpty()) {
            throw CamundaError.bpmnError("NO_AGENT", "Aucun agent disponible");
        }

        var agent = agents.get(ThreadLocalRandom.current().nextInt(agents.size()));
        ticketClient.assign(ticketId, Map.of("agentId", agent.id()));

        log.info("Ticket {} assigned to agent {}", ticketId, agent.name());
        return Map.of("agentId", agent.id());   // devient une variable du process
    }
}