package com.ticketflow.ticketservice.repository;

import com.ticketflow.ticketservice.model.PriorityChange;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriorityChangeRepository extends JpaRepository<PriorityChange, Long> {
    List<PriorityChange> findByTicketIdOrderByChangedAtAsc(Long ticketId);
}