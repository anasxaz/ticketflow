package com.ticketflow.ticketservice.outbox;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ticketflow.ticketservice.config.KafkaTopicConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 2000) // toutes les 2 s après la fin du tour précédent
    @Transactional 
    public void relay() {
        for (OutboxEvent event : outboxRepository.lockNextBatch()) {
            try {
                kafkaTemplate.send(KafkaTopicConfig.TICKET_EVENTS,
                                event.getAggregateId().toString(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);          // on attend la confirmation de Kafka

                event.setSentAt(Instant.now());
                outboxRepository.save(event);
                log.info("📤 Outbox → Kafka: {} for ticket {}", event.getType(), event.getAggregateId());
            } catch (Exception e) {
                log.warn("⏳ Kafka unavailable, will retry: {}", e.getMessage());
                break;   // on garde l'ordre : on réessaiera au prochain tour
            }
        }
    }
}
