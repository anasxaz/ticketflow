package com.ticketflow.workflowservice;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.openfeign.EnableFeignClients;

@ConfigurationPropertiesScan
@SpringBootApplication
@Deployment(resources = {"classpath:bpmn/ticket-lifecycle.bpmn", "classpath:bpmn/triage-form.form"})
@EnableFeignClients
public class WorkflowServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WorkflowServiceApplication.class, args);
	}
}


