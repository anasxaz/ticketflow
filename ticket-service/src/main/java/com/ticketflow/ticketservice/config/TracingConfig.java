package com.ticketflow.ticketservice.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TracingConfig {

    // Ne pas tracer les tâches @Scheduled (le relay Outbox tourne toutes les 2 s)
    @Bean
    public ObservationPredicate ignoreScheduledTasks() {
        return (name, context) -> !name.equals("tasks.scheduled.execution");
    }
}