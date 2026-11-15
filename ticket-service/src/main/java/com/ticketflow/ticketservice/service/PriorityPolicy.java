package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.model.Priority;
import com.ticketflow.ticketservice.model.TicketCategory;
import org.springframework.stereotype.Component;

@Component
public class PriorityPolicy {

    /** Matrice ITIL : impact x urgence, puis +1 cran pour un client ENTREPRISE. */
    public Priority compute(TicketCategory category, boolean blocked, String customerType) {
        Priority base = switch (category.getImpact()) {
            case FORT   -> blocked ? Priority.CRITICAL : Priority.HIGH;
            case MOYEN  -> blocked ? Priority.HIGH     : Priority.MEDIUM;
            case FAIBLE -> blocked ? Priority.MEDIUM   : Priority.LOW;
        };
        return "ENTREPRISE".equals(customerType) ? upgrade(base) : base;
    }

    // Priority = LOW, MEDIUM, HIGH, CRITICAL (l'ordre de l'enum compte !)
    private Priority upgrade(Priority p) {
        Priority[] all = Priority.values();
        return all[Math.min(p.ordinal() + 1, all.length - 1)];
    }
}