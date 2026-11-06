package com.ticketflow.ticketservice.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "outbox_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {
    @Id
    private String id;  //String pour un UUID

    private Long aggregateId;
    private String type;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private Instant createdAt;
    private Instant sentAt;
}
