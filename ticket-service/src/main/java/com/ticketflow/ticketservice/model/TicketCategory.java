package com.ticketflow.ticketservice.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TicketCategory {
    PANNE_TOTALE(Impact.FORT),        // plus de service du tout
    SERVICE_DEGRADE(Impact.MOYEN),    // lent, coupures
    FACTURATION(Impact.FAIBLE),
    DEMANDE_INFO(Impact.FAIBLE);

    private final Impact impact;      // chaque categorie "sait" son impact
}