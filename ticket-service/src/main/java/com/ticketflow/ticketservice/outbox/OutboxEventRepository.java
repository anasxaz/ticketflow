package com.ticketflow.ticketservice.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {

    List<OutboxEvent> findTop100BySentAtIsNullOrderByCreatedAtAsc();   // utilisee par les tests

    /** Verrouille le prochain lot ; les lignes deja verrouillees par un autre pod sont SAUTEES. */
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE sent_at IS NULL
            ORDER BY created_at
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextBatch();
}