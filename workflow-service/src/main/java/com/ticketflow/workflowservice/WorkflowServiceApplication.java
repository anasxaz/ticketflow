package com.ticketflow.workflowservice;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@Deployment(resources = "classpath*:/bpmn/*.bpmn")
@EnableFeignClients
public class WorkflowServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(WorkflowServiceApplication.class, args);
	}
}