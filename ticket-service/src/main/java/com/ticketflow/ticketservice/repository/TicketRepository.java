package com.ticketflow.ticketservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ticketflow.ticketservice.model.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByClientIdOrderByCreatedAtDesc(Long clientId);   // requete generee par Spring Data
}