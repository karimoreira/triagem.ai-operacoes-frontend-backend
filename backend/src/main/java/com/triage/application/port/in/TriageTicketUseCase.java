package com.triage.application.port.in;

import com.triage.domain.entity.Ticket;

/**
 * Port de entrada (use case boundary). A camada de interface (HTTP)
 * depende deste contrato, não da implementação concreta.
 */
public interface TriageTicketUseCase {
    Ticket handle(Command command);

    /** Dados de entrada do caso de uso. */
    record Command(String customerId, String subject, String body) {
    }
}
