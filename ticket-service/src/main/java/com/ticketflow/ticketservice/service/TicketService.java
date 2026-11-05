package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.model.Ticket;
import com.ticketflow.ticketservice.model.TicketStatus;
import com.ticketflow.ticketservice.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {
    private final TicketRepository ticketRepository;

    public List<Ticket> findAll(){
        return ticketRepository.findAll();
    }

    public Ticket findById(Long id){
        return ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found : "+id));
    }

    public Ticket create(Ticket ticket){
        return ticketRepository.save(ticket);
    }

    public Ticket updateStatus(Long id, TicketStatus status){
        Ticket ticket = findById(id);   //retrieve the ticket
        ticket.setStatus(status);   //modify the ticket's status
        return ticketRepository.save(ticket);
    }

    public void delete(Long id){
        ticketRepository.deleteById(id);
    }
}
