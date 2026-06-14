package com.triage.domain.entity;

import com.triage.domain.valueobject.Category;
import com.triage.domain.valueobject.Priority;

import java.util.List;

/**
 * Decisão produzida pelo agente de IA ao analisar um ticket.
 * Objeto imutável: representa o veredito da triagem em um instante.
 */
public final class TriageDecision {

    private final Priority priority;
    private final Category category;
    private final boolean shouldEscalate;
    private final String suggestedReply;
    private final String reasoning;
    private final List<String> executedActions;

    public TriageDecision(Priority priority, Category category, boolean shouldEscalate,
                          String suggestedReply, String reasoning, List<String> executedActions) {
        this.priority = priority;
        this.category = category;
        this.shouldEscalate = shouldEscalate;
        this.suggestedReply = suggestedReply;
        this.reasoning = reasoning;
        this.executedActions = executedActions == null ? List.of() : List.copyOf(executedActions);
    }

    public Priority priority() {
        return priority;
    }

    public Category category() {
        return category;
    }

    public boolean shouldEscalate() {
        return shouldEscalate;
    }

    public String suggestedReply() {
        return suggestedReply;
    }

    public String reasoning() {
        return reasoning;
    }

    public List<String> executedActions() {
        return executedActions;
    }
}
