package com.triage.application.agent.tool;

import com.triage.application.agent.AgentTool;
import com.triage.application.port.out.LLMProvider.ToolSpec;
import com.triage.application.port.out.NotificationGateway;
import com.triage.infrastructure.config.Json;

import java.util.Map;

/**
 * Ferramenta que permite ao agente escalar um ticket para atendimento
 * humano, disparando uma notificação (ex.: Slack).
 */
public final class EscalateTool implements AgentTool {

    private final NotificationGateway notificationGateway;

    public EscalateTool(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    @Override
    public String name() {
        return "escalate_to_human";
    }

    @Override
    public ToolSpec specification() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "ticketId": { "type": "string" },
                "reason": { "type": "string", "description": "Por que precisa de humano" }
              },
              "required": ["ticketId", "reason"]
            }
            """;
        return new ToolSpec(name(),
            "Escala o ticket para um atendente humano quando o caso é sensível, "
                + "complexo ou de alta criticidade.",
            schema);
    }

    @Override
    public String execute(String argumentsJson) {
        Map<String, Object> args = Json.toMap(argumentsJson);
        String ticketId = String.valueOf(args.getOrDefault("ticketId", "desconhecido"));
        String reason = String.valueOf(args.getOrDefault("reason", "não informado"));

        notificationGateway.notifyEscalation(ticketId, reason);
        return "Ticket " + ticketId + " escalado. Motivo: " + reason;
    }
}
