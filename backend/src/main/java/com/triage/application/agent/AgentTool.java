package com.triage.application.agent;

import com.triage.application.port.out.LLMProvider.ToolSpec;

/**
 * Contrato comum a toda ferramenta que o agente pode executar.
 * Princípio Open/Closed: novas capacidades do agente entram como novas
 * implementações desta interface, sem alterar o orquestrador.
 */
public interface AgentTool {

    /** Nome único da ferramenta (usado pelo modelo para invocá-la). */
    String name();

    /** Especificação exposta ao modelo (descrição + schema dos argumentos). */
    ToolSpec specification();

    /**
     * Executa a ferramenta com os argumentos em JSON fornecidos pelo modelo.
     * @return texto de resultado que será devolvido ao modelo.
     */
    String execute(String argumentsJson);
}
