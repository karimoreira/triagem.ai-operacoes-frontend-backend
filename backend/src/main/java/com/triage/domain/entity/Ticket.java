package com.triage.domain.entity;

import com.triage.domain.exception.DomainException;
import com.triage.domain.valueobject.Category;
import com.triage.domain.valueobject.Priority;
import com.triage.domain.valueobject.TicketStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidade central do domínio. Representa um ticket de suporte e protege
 * suas próprias invariantes: o estado só muda por métodos de negócio,
 * nunca por setters abertos. Não conhece banco de dados, HTTP nem IA.
 */
public final class Ticket {

    private final String id;
    private final String customerId;
    private final String subject;
    private final String body;
    private final Instant createdAt;

    private TicketStatus status;
    private Priority priority;
    private Category category;
    private String reasoning;
    private String suggestedReply;
    private java.util.List<String> executedActions;

    private Ticket(String id, String customerId, String subject, String body, Instant createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.subject = subject;
        this.body = body;
        this.createdAt = createdAt;
        this.status = TicketStatus.RECEIVED;
        this.executedActions = java.util.List.of();
    }

    /** Factory para criação de um ticket novo, com validação das invariantes. */
    public static Ticket create(String customerId, String subject, String body) {
        if (customerId == null || customerId.isBlank()) {
            throw new DomainException("customerId é obrigatório");
        }
        if (subject == null || subject.isBlank()) {
            throw new DomainException("subject é obrigatório");
        }
        if (body == null || body.isBlank()) {
            throw new DomainException("body é obrigatório");
        }
        return new Ticket(UUID.randomUUID().toString(), customerId, subject, body, Instant.now());
    }

    /** Factory para reidratação a partir da persistência (sem regerar id/data). */
    public static Ticket rehydrate(String id, String customerId, String subject, String body,
                                   Instant createdAt, TicketStatus status,
                                   Priority priority, Category category,
                                   String reasoning, String suggestedReply, java.util.List<String> executedActions) {
        Ticket ticket = new Ticket(id, customerId, subject, body, createdAt);
        ticket.status = status;
        ticket.priority = priority;
        ticket.category = category;
        ticket.reasoning = reasoning;
        ticket.suggestedReply = suggestedReply;
        ticket.executedActions = executedActions == null ? java.util.List.of() : java.util.List.copyOf(executedActions);
        return ticket;
    }

    public void markTriaging() {
        if (status != TicketStatus.RECEIVED && status != TicketStatus.FAILED) {
            throw new DomainException("Só é possível iniciar triagem de um ticket recebido");
        }
        this.status = TicketStatus.TRIAGING;
    }

    public void applyTriage(Priority priority, Category category, boolean escalated,
                            String reasoning, String suggestedReply, java.util.List<String> executedActions) {
        if (priority == null || category == null) {
            throw new DomainException("Triagem requer prioridade e categoria");
        }
        this.priority = priority;
        this.category = category;
        if (escalated) {
            this.status = TicketStatus.ESCALATED;
        } else if (suggestedReply != null && !suggestedReply.isBlank()) {
            this.status = TicketStatus.AUTO_RESOLVED;
        } else {
            this.status = TicketStatus.TRIAGED;
        }
        this.reasoning = reasoning;
        this.suggestedReply = suggestedReply;
        this.executedActions = executedActions == null ? java.util.List.of() : java.util.List.copyOf(executedActions);
    }

    public void markAutoResolved() {
        this.status = TicketStatus.AUTO_RESOLVED;
    }

    public void markFailed() {
        this.status = TicketStatus.FAILED;
    }

    public String id() {
        return id;
    }

    public String customerId() {
        return customerId;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public TicketStatus status() {
        return status;
    }

    public Priority priority() {
        return priority;
    }

    public Category category() {
        return category;
    }

    public String reasoning() {
        return reasoning;
    }

    public String suggestedReply() {
        return suggestedReply;
    }

    public java.util.List<String> executedActions() {
        return executedActions;
    }
}
