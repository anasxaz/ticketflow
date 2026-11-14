package com.ticketflow.notificationservice;

import com.ticketflow.notificationservice.event.TicketEvent;
import com.ticketflow.notificationservice.kafka.TicketEventListener;
import com.ticketflow.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class TicketEventListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired TicketEventListener listener;
    @Autowired NotificationRepository notificationRepository;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired JsonMapper jsonMapper;

    @BeforeEach
    void cleanDatabase() {
        notificationRepository.deleteAll();
    }

    private String eventJson(String eventId, Long ticketId) {
        return jsonMapper.writeValueAsString(new TicketEvent(
                eventId, "TICKET_CREATED", ticketId, 2L, "Pas de connexion", "OPEN", Instant.now()));
    }

    // Test 1 : on appelle le listener directement, 2 fois avec le MEME eventId
    @Test
    void sameEventTwice_createsOnlyOneNotification() {
        String json = eventJson(UUID.randomUUID().toString(), 100L);

        listener.onTicketEvent(json);
        listener.onTicketEvent(json);          // doublon (comme une relivraison Kafka)

        assertThat(notificationRepository.count()).isEqualTo(1);
    }

    // Test 2 : de bout en bout, a travers le VRAI Kafka
    @Test
    void duplicateKafkaMessage_isIgnored_endToEnd() {
        String duplicated = eventJson(UUID.randomUUID().toString(), 200L);
        String marker     = eventJson(UUID.randomUUID().toString(), 200L);   // un 3e evenement, DIFFERENT

        // meme cle "200" -> meme partition -> traites DANS L'ORDRE
        kafkaTemplate.send("ticket-events", "200", duplicated);
        kafkaTemplate.send("ticket-events", "200", duplicated);   // doublon
        kafkaTemplate.send("ticket-events", "200", marker);

        // Quand le "marker" est traite, on est SUR que le doublon l'a ete avant lui
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(notificationRepository.count()).isEqualTo(2));   // 1 (doublon ignore) + 1 (marker)
    }
}