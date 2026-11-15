package com.ticketflow.ticketservice.service;

import com.ticketflow.ticketservice.model.Priority;
import com.ticketflow.ticketservice.model.TicketCategory;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PriorityPolicyTest {

    private final PriorityPolicy policy = new PriorityPolicy();   // pas de Spring : rapide

    @ParameterizedTest(name = "{0} / bloque={1} / {2} -> {3}")
    @CsvSource({
            "PANNE_TOTALE,    true,  PARTICULIER, CRITICAL",
            "PANNE_TOTALE,    false, PARTICULIER, HIGH",
            "SERVICE_DEGRADE, true,  PARTICULIER, HIGH",
            "SERVICE_DEGRADE, false, PARTICULIER, MEDIUM",
            "FACTURATION,     false, PARTICULIER, LOW",
            "FACTURATION,     false, ENTREPRISE,  MEDIUM",     // +1 cran
            "PANNE_TOTALE,    true,  ENTREPRISE,  CRITICAL"    // deja au max
    })
    void computesPriority(TicketCategory category, boolean blocked, String type, Priority expected) {
        assertThat(policy.compute(category, blocked, type)).isEqualTo(expected);
    }
}