package com.triage.domain.valueobject;

/**
 * Ciclo de vida de um ticket dentro do sistema de triagem.
 */
public enum TicketStatus {
    /** Recém-criado, ainda não processado pelo agente. */
    RECEIVED,
    /** Em processamento pelo agente de IA. */
    TRIAGING,
    /** Triagem concluída com decisão registrada. */
    TRIAGED,
    /** Escalado para atendimento humano. */
    ESCALATED,
    /** Resolvido automaticamente. */
    AUTO_RESOLVED,
    /** Falha durante a triagem. */
    FAILED
}
