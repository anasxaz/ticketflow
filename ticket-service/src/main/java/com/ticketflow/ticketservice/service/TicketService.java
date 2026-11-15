package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.ai.TicketClassifier;
import com.ticketflow.ticketservice.client.UserClient;
import com.ticketflow.ticketservice.dto.CreateTicketRequest;
import com.ticketflow.ticketservice.dto.RequalifyRequest;
import com.ticketflow.ticketservice.dto.UserResponse;
import com.ticketflow.ticketservice.event.TicketEventPublisher;
import com.ticketflow.ticketservice.model.*;
import com.ticketflow.ticketservice.repository.PriorityChangeRepository;
import com.ticketflow.ticketservice.repository.TicketRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserClient userClient;

    private final TicketEventPublisher eventPublisher;  //après avoir ajouté kafka pour la notification
    private final PriorityPolicy priorityPolicy;
    private final PriorityChangeRepository priorityChangeRepository;

    private final TicketClassifier ticketClassifier;

    @Value("${ticketflow.triage.confidence-threshold:0.7}")
    private double confidenceThreshold;           // pas final : injecte par @Value

    public List<Ticket> findAll(){
        return ticketRepository.findAll();
    }

    public Ticket findById(Long id){
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Ticket not found : "+id));
    }

//    public Ticket create(Ticket ticket){
//        return ticketRepository.save(ticket);
//    }

//    @Transactional
//    public Ticket create(Ticket ticket) {
//        try {
//            UserResponse user = userClient.getUserById(ticket.getClientId());
//            if (!"CLIENT".equals(user.role())) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                        "User " + user.id() + " is not a CLIENT");
//            }
//        } catch (FeignException.NotFound e) {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                    "Client not found: " + ticket.getClientId());
//        }
//
//        Ticket saved = ticketRepository.save(ticket);           // 1. sauvegarde
//        eventPublisher.publish("TICKET_CREATED", saved);   // 2. publie l'événement
//        return saved;                                                 // 3. renvoie le ticket
//    }

    @Transactional
    public Ticket create(CreateTicketRequest request, String email) {
        UserResponse user;
        try {
            user = userClient.getUserByEmail(email);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No user profile for " + email);
        }
        if (!"CLIENT".equals(user.role())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User " + user.id() + " is not a CLIENT");
        }

        Ticket ticket = new Ticket();
        ticket.setTitle(request.title());
        ticket.setDescription(request.description());
//        ticket.setCategory(request.category());
//        ticket.setBlocked(request.blocked());
//        ticket.setPriority(priorityPolicy.compute(        // est CALCULEE, plus choisie
//                request.category(), request.blocked(), user.customerType()));
        triage(ticket, request, user.customerType());
        ticket.setClientId(user.id());               // ça vient du TOKEN, pas du body

        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish("TICKET_CREATED", saved);
        return saved;
    }

    @Transactional
    public Ticket updateStatus(Long id, TicketStatus status){
        Ticket ticket = findById(id);   //retrieve the ticket
        ticket.setStatus(status);   //modify the ticket's status
        Ticket saved = ticketRepository.save(ticket);           // 1. sauvegarde
        eventPublisher.publish("STATUS_CHANGED", saved);   // 2. publie l'événement
        return saved;                                                 // 3. renvoie le ticket
    }


    public void delete(Long id){
        ticketRepository.deleteById(id);
    }

    @Transactional
    public Ticket assign(Long id, Long agentId){
        Ticket ticket = findById(id);
        ticket.setAgentId(agentId);
        ticket.setStatus(TicketStatus.ASSIGNED);
        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish("STATUS_CHANGED", saved);  //pour faire une notification au client
        return saved;
    }

    @Transactional
    public Ticket requalify(Long id, RequalifyRequest request, String email) {
        Ticket ticket = findById(id);

        // 1. Securite : seul l'AGENT assigne a CE ticket peut le requalifier
        UserResponse user;
        try {
            user = userClient.getUserByEmail(email);
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No user profile for " + email);
        }
        if (!"AGENT".equals(user.role()) || !user.id().equals(ticket.getAgentId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the assigned agent can requalify this ticket");
        }
        if (ticket.getPriority() == request.priority()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket already has priority " + request.priority());
        }

        // 2. Audit : on garde la trace AVANT de modifier
        priorityChangeRepository.save(PriorityChange.builder()
                .ticketId(id)
                .oldPriority(ticket.getPriority())
                .newPriority(request.priority())
                .reason(request.reason())
                .changedBy(email)
                .changedAt(Instant.now())
                .build());

        // 3. Modification + evenement (meme transaction : Outbox)
        ticket.setPriority(request.priority());
        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publish("PRIORITY_CHANGED", saved);
        return saved;
    }

    public List<PriorityChange> priorityHistory(Long id) {
        findById(id);                                   // 404 si le ticket n'existe pas
        return priorityChangeRepository.findByTicketIdOrderByChangedAtAsc(id);
    }

    /** Couche 1 (comprendre) + couche 2 (decider). */
    private void triage(Ticket ticket, CreateTicketRequest request, String customerType) {
        // V1 : le client a repondu au formulaire
        if (request.category() != null && request.blocked() != null) {
            applyRules(ticket, request.category(), request.blocked(), customerType);
            ticket.setTriageSource(TriageSource.FORM);
            ticket.setNeedsReview(false);
            return;
        }

        // V2 : texte libre -> le LLM extrait les faits
        if (request.description() == null || request.description().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide category + blocked, or a description");
        }
        Optional<TicketClassifier.Classification> ai = ticketClassifier.classify(request.title(), request.description());

        if (ai.isPresent() && ai.get().confidence() >= confidenceThreshold && ai.get().confidence() <= 1) {
            TicketClassifier.Classification c = ai.get();
            applyRules(ticket, c.category(), c.blocked(), customerType);   // les REGLES decident
            ticket.setTriageSource(TriageSource.AI);
            ticket.setAiConfidence(c.confidence());
            ticket.setNeedsReview(false);
        } else {
            // LLM pas sur, invalide ou en panne : on ne devine pas -> un humain triera
            ticket.setPriority(Priority.MEDIUM);
            ticket.setTriageSource(TriageSource.FALLBACK);
            ai.ifPresent(c -> ticket.setAiConfidence(c.confidence()));
            ticket.setNeedsReview(true);
        }
    }

    private void applyRules(Ticket ticket, TicketCategory category, boolean blocked, String customerType) {
        ticket.setCategory(category);
        ticket.setBlocked(blocked);
        ticket.setPriority(priorityPolicy.compute(category, blocked, customerType));
    }
}
