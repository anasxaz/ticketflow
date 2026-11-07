package com.ticketflow.workflowservice;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Deployment(resources = "classpath*:/bpmn/*.bpmn")
public class WorkflowServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WorkflowServiceApplication.class, args);
	}
}