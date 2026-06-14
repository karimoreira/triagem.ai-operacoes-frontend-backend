package com.triage.application.agent;

import com.triage.application.port.out.LLMProvider;
import com.triage.application.port.out.LLMProvider.LLMMessage;
import com.triage.application.port.out.LLMProvider.LLMResponse;
import com.triage.application.port.out.LLMProvider.ToolCall;
import com.triage.domain.entity.Ticket;
import com.triage.domain.entity.TriageDecision;
import com.triage.domain.valueobject.Category;
import com.triage.domain.valueobject.Priority;
import com.triage.infrastructure.config.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador do agente de IA. Implementa o laço característico de um
 * agente: o modelo decide, eventualmente pede para usar ferramentas,
 * executamos as ferramentas e devolvemos o resultado, repetindo até o
 * modelo produzir uma resposta final. Há um teto de iterações para evitar
 * laços infinitos.
 */
public final class TriageAgent {

    private static final int MAX_ITERATIONS = 5;

    private final LLMProvider llm;
    private final ToolRegistry toolRegistry;

    public TriageAgent(LLMProvider llm, ToolRegistry toolRegistry) {
        this.llm = llm;
        this.toolRegistry = toolRegistry;
    }

    public TriageDecision triage(Ticket ticket) {
        List<LLMMessage> conversation = new ArrayList<>();
        conversation.add(new LLMMessage(LLMMessage.Role.SYSTEM, systemPrompt()));
        conversation.add(new LLMMessage(LLMMessage.Role.USER, userPrompt(ticket)));

        List<String> executedActions = new ArrayList<>();

        for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
            LLMResponse response = llm.chat(conversation, toolRegistry.specifications());

            if (response.requiresToolExecution()) {
                conversation.add(new LLMMessage(LLMMessage.Role.ASSISTANT,
                    "Solicitando execução de ferramentas."));
                for (ToolCall call : response.toolCalls()) {
                    String result = toolRegistry.execute(call.name(), call.argumentsJson());
                    executedActions.add(call.name());
                    conversation.add(new LLMMessage(LLMMessage.Role.TOOL,
                        "Resultado de " + call.name() + ": " + result));
                }
                continue;
            }

            return parseFinalDecision(response.text(), executedActions);
        }

        // Excedeu o teto: devolve decisão conservadora (escala para humano).
        return new TriageDecision(Priority.HIGH, Category.OTHER, true,
            null, "Limite de iterações atingido; escalado por segurança.", executedActions);
    }

    private TriageDecision parseFinalDecision(String json, List<String> executedActions) {
        Map<String, Object> parsed = Json.toMap(json);
        Priority priority = Priority.fromString(asString(parsed.get("priority")));
        Category category = Category.fromString(asString(parsed.get("category")));
        boolean escalate = Boolean.parseBoolean(asString(parsed.get("shouldEscalate")));
        String reply = asString(parsed.get("suggestedReply"));
        String reasoning = asString(parsed.get("reasoning"));
        return new TriageDecision(priority, category, escalate, reply, reasoning, executedActions);
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String systemPrompt() {
        return """
            Você é um agente de triagem de tickets de suporte. Analise o ticket,
            classifique prioridade e categoria, e decida se precisa de ação humana.
            Use a ferramenta create_task para abrir uma tarefa de acompanhamento
            quando houver trabalho a fazer, e escalate_to_human para casos sensíveis
            ou críticos. Quando concluir, responda APENAS com um JSON no formato:
            {
              "priority": "LOW|MEDIUM|HIGH|CRITICAL",
              "category": "BILLING|TECHNICAL|ACCOUNT|FEEDBACK|OTHER",
              "shouldEscalate": true|false,
              "suggestedReply": "resposta sugerida ao cliente",
              "reasoning": "justificativa curta da decisão"
            }
            Não inclua texto fora do JSON na resposta final.
            """;
    }

    private String userPrompt(Ticket ticket) {
        return "Ticket id: " + ticket.id() + "\n"
            + "Assunto: " + ticket.subject() + "\n"
            + "Mensagem: " + ticket.body();
    }
}
