package com.ticketflow.ticketservice.controller;

import com.ticketflow.ticketservice.model.Ticket;
import com.ticketflow.ticketservice.model.TicketStatus;
import com.ticketflow.ticketservice.repository.TicketRepository;
import com.ticketflow.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    //un petit DTO pour l'update du status
    public record StatusRequest(TicketStatus status){}

    @GetMapping
    public List<Ticket> getAll(){
        return ticketService.findAll();
    }

    @GetMapping("/{id}")
    public Ticket getById(@PathVariable Long id){
        return ticketService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket create(@Valid @RequestBody Ticket ticket){
        return ticketService.create(ticket);
    }

    @PatchMapping("/{id}/status")
    public Ticket updateStatus(@PathVariable Long id, @RequestBody StatusRequest request){
        return ticketService.updateStatus(id, request.status);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id){
        ticketService.delete(id);
    }
}
