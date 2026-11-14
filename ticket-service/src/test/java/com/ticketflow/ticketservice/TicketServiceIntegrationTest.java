package com.ticketflow.ticketservice;

import com.ticketflow.ticketservice.client.UserClient;
import com.ticketflow.ticketservice.dto.CreateTicketRequest;
import com.ticketflow.ticketservice.dto.UserResponse;
import com.ticketflow.ticketservice.event.TicketEventPublisher;
import com.ticketflow.ticketservice.model.Priority;
import com.ticketflow.ticketservice.model.Ticket;
import com.ticketflow.ticketservice.model.TicketStatus;
import com.ticketflow.ticketservice.outbox.OutboxEvent;
import com.ticketflow.ticketservice.outbox.OutboxEventRepository;
import com.ticketflow.ticketservice.repository.TicketRepository;
import com.ticketflow.ticketservice.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class TicketServiceIntegrationTest extends AbstractIntegrationTest {

    private static final String EMAIL = "youssef@test.com";

    @MockitoBean            // user-service est SIMULE (on teste ticket-service seul)
    UserClient userClient;

    @MockitoSpyBean         // le VRAI publisher, mais qu'on peut faire echouer a la demande
    TicketEventPublisher eventPublisher;

    @Autowired TicketService ticketService;
    @Autowired TicketRepository ticketRepository;
    @Autowired OutboxEventRepository outboxRepository;

    @BeforeEach
    void cleanDatabase() {      // chaque test part d'une base vide
        outboxRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    private CreateTicketRequest request() {
        return new CreateTicketRequest("Pas de connexion", "Box eteinte", Priority.HIGH);
    }

    @Test
    void create_savesTicketAndOutboxEvent_inSameTransaction() {
        // GIVEN : user-service dit "c'est un CLIENT, id 2"
        when(userClient.getUserByEmail(EMAIL))
                .thenReturn(new UserResponse(2L, "Youssef", EMAIL, "CLIENT"));

        // WHEN
        Ticket saved = ticketService.create(request(), EMAIL);

        // THEN 1 : le ticket est VRAIMENT en base MariaDB
        Ticket inDb = ticketRepository.findById(saved.getId()).orElseThrow();
        assertThat(inDb.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(inDb.getClientId()).isEqualTo(2L);          // vient du "token", pas du body

        // THEN 2 : l'evenement est dans la table outbox
        List<OutboxEvent> events = outboxRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getType()).isEqualTo("TICKET_CREATED");
        assertThat(events.get(0).getAggregateId()).isEqualTo(saved.getId());
    }

    @Test
    void create_isRejected_whenUserIsNotClient() {
        // GIVEN : l'utilisateur est un AGENT
        when(userClient.getUserByEmail(EMAIL))
                .thenReturn(new UserResponse(1L, "Anas", EMAIL, "AGENT"));

        // WHEN / THEN : refuse...
        assertThatThrownBy(() -> ticketService.create(request(), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("is not a CLIENT");

        // ... et RIEN n'est ecrit
        assertThat(ticketRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }

    @Test
    void create_rollsBackTicket_whenOutboxWriteFails() {
        // GIVEN : client valide, MAIS l'ecriture de l'evenement va echouer
        when(userClient.getUserByEmail(EMAIL))
                .thenReturn(new UserResponse(2L, "Youssef", EMAIL, "CLIENT"));
        doThrow(new RuntimeException("Echec ecriture outbox"))
                .when(eventPublisher).publish(eq("TICKET_CREATED"), any(Ticket.class));

        // WHEN : la creation echoue
        assertThatThrownBy(() -> ticketService.create(request(), EMAIL))
                .hasMessageContaining("Echec ecriture outbox");

        // THEN : le ticket a ete ANNULE (rollback) -> pas de ticket sans evenement
        assertThat(ticketRepository.count()).isZero();
        assertThat(outboxRepository.count()).isZero();
    }
}