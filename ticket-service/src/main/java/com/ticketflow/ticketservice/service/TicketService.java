package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.client.UserClient;
import com.ticketflow.ticketservice.dto.CreateTicketRequest;
import com.ticketflow.ticketservice.dto.UserResponse;
import com.ticketflow.ticketservice.event.TicketEventPublisher;
import com.ticketflow.ticketservice.model.Ticket;
import com.ticketflow.ticketservice.model.TicketStatus;
import com.ticketflow.ticketservice.repository.TicketRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;
    private final UserClient userClient;

    private final TicketEventPublisher eventPublisher;  //après avoir ajouté kafka pour la notification

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
        ticket.setPriority(request.priority());
        ticket.setClientId(user.id());               // 👈 vient du TOKEN, pas du body

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
}
