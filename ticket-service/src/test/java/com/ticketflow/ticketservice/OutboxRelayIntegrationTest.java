package com.ticketflow.ticketservice;

import com.ticketflow.ticketservice.client.UserClient;
import com.ticketflow.ticketservice.dto.CreateTicketRequest;
import com.ticketflow.ticketservice.dto.UserResponse;
import com.ticketflow.ticketservice.model.Priority;
import com.ticketflow.ticketservice.model.Ticket;
import com.ticketflow.ticketservice.outbox.OutboxEventRepository;
import com.ticketflow.ticketservice.repository.TicketRepository;
import com.ticketflow.ticketservice.service.TicketService;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

class OutboxRelayIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "youssef@test.com";

    //permet de remplacer un userclient par un faux
    @MockitoBean UserClient userClient;

    @Autowired TicketService ticketService;
    @Autowired TicketRepository ticketRepository;
    @Autowired OutboxEventRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {
        outboxRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    @Test
    void relay_publishesEventToKafka_andMarksItAsSent() {
        // GIVEN
        when(userClient.getUserByEmail(EMAIL))
                .thenReturn(new UserResponse(2L, "Youssef", EMAIL, "CLIENT"));

        // WHEN : on cree un ticket (l'evenement va dans la table outbox)
        Ticket saved = ticketService.create(
                new CreateTicketRequest("Test relay", "Kafka", Priority.LOW), EMAIL);
        String ticketId = String.valueOf(saved.getId());

        // THEN 1 : un VRAI consumer Kafka recoit l'evenement (dans les 15 s)
        try (KafkaConsumer<String, String> consumer = testConsumer()) {
            consumer.subscribe(List.of("ticket-events"));
            List<ConsumerRecord<String, String>> received = new ArrayList<>();

            await().atMost(Duration.ofSeconds(15)).until(() -> {
                consumer.poll(Duration.ofMillis(500)).forEach(received::add);
                return received.stream().anyMatch(r -> ticketId.equals(r.key()));
            });

            ConsumerRecord<String, String> record = received.stream()
                    .filter(r -> ticketId.equals(r.key()))
                    .findFirst().orElseThrow();
            assertThat(record.value()).contains("TICKET_CREATED");   // le bon type
            assertThat(record.key()).isEqualTo(ticketId);             // cle = ticketId -> ordre garanti
        }

        // THEN 2 : la ligne outbox est marquee "envoyee"
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(outboxRepository.findAll())
                        .allSatisfy(e -> assertThat(e.getSentAt()).isNotNull()));
    }

    // Un consumer Kafka "de test", branche sur le Kafka de Testcontainers
    private KafkaConsumer<String, String> testConsumer() {
        return new KafkaConsumer<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),   // groupe unique
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",           // lire depuis le debut
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
    }
}