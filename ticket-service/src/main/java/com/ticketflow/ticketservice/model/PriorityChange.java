package com.ticketflow.ticketservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "priority_changes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriorityChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ticketId;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private Priority oldPriority;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private Priority newPriority;

    private String reason;
    private String changedBy;      // email de l'agent (vient du token)
    private Instant changedAt;
}