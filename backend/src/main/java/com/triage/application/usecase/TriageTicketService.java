package com.triage.application.usecase;

import com.triage.application.agent.TriageAgent;
import com.triage.application.port.in.TriageTicketUseCase;
import com.triage.application.port.out.TicketRepository;
import com.triage.domain.entity.Ticket;
import com.triage.domain.entity.TriageDecision;

/**
 * Orquestra o fluxo de triagem: cria o ticket, aciona o agente de IA,
 * aplica a decisão ao ticket e persiste. Depende apenas de abstrações
 * (port do agente, port do repositório) — nada de frameworks ou IO direto.
 */
public final class TriageTicketService implements TriageTicketUseCase {

    private final TicketRepository ticketRepository;
    private final TriageAgent triageAgent;

    public TriageTicketService(TicketRepository ticketRepository, TriageAgent triageAgent) {
        this.ticketRepository = ticketRepository;
        this.triageAgent = triageAgent;
    }

    @Override
    public Ticket handle(Command command) {
        Ticket ticket = Ticket.create(command.customerId(), command.subject(), command.body());
        ticketRepository.save(ticket);

        Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(100);
                ticket.markTriaging();
                ticketRepository.save(ticket);
                TriageDecision decision = triageAgent.triage(ticket);
                ticket.applyTriage(decision.priority(), decision.category(), decision.shouldEscalate(),
                    decision.reasoning(), decision.suggestedReply(), decision.executedActions());
            } catch (RuntimeException | InterruptedException ex) {
                Thread.currentThread().interrupt();
                ticket.markFailed();
            }
            ticketRepository.save(ticket);
        });

        return ticket;
    }
}
