package com.ticketflow.workflowservice.worker;

import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AssignAgentWorker {

    @JobWorker(type = "assign-agent")
    public void assignAgent(@Variable Long ticketId){
        log.info("Job assign-agent for ticket {}", ticketId);
    }
}
