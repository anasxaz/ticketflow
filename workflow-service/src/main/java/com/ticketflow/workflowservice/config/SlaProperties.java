package com.ticketflow.workflowservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

/** Lit ticketflow.sla.CRITICAL=PT1M, ticketflow.sla.HIGH=PT2M, ... */
@ConfigurationProperties(prefix = "ticketflow")
public record SlaProperties(Map<String, Duration> sla) {
}