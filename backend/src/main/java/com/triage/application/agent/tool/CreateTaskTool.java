package com.triage.application.agent.tool;

import com.triage.application.agent.AgentTool;
import com.triage.application.port.out.LLMProvider.ToolSpec;
import com.triage.application.port.out.TaskManagerGateway;
import com.triage.infrastructure.config.Json;

import java.util.Map;

/**
 * Ferramenta que permite ao agente criar uma tarefa em um gerenciador
 * externo. O agente decide quando usá-la; aqui apenas executamos.
 */
public final class CreateTaskTool implements AgentTool {

    private final TaskManagerGateway taskManager;

    public CreateTaskTool(TaskManagerGateway taskManager) {
        this.taskManager = taskManager;
    }

    @Override
    public String name() {
        return "create_task";
    }

    @Override
    public ToolSpec specification() {
        String schema = """
            {
              "type": "object",
              "properties": {
                "title": { "type": "string", "description": "Título curto da tarefa" },
                "description": { "type": "string", "description": "Descrição detalhada" },
                "priority": { "type": "string", "enum": ["LOW","MEDIUM","HIGH","CRITICAL"] }
              },
              "required": ["title", "description", "priority"]
            }
            """;
        return new ToolSpec(name(),
            "Cria uma tarefa de acompanhamento para a equipe quando o ticket exige ação humana.",
            schema);
    }

    @Override
    public String execute(String argumentsJson) {
        Map<String, Object> args = Json.toMap(argumentsJson);
        String title = String.valueOf(args.getOrDefault("title", "Sem título"));
        String description = String.valueOf(args.getOrDefault("description", ""));
        String priority = String.valueOf(args.getOrDefault("priority", "MEDIUM"));

        String taskId = taskManager.createTask(title, description, priority);
        return "Tarefa criada com id=" + taskId;
    }
}
