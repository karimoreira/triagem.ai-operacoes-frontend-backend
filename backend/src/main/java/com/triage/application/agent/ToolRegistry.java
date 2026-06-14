package com.triage.application.agent;

import com.triage.application.port.out.LLMProvider.ToolSpec;
import com.triage.domain.exception.DomainException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registro central das ferramentas disponíveis ao agente.
 * Resolve uma ferramenta por nome e expõe suas especificações ao modelo.
 */
public final class ToolRegistry {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistry(List<AgentTool> availableTools) {
        for (AgentTool tool : availableTools) {
            tools.put(tool.name(), tool);
        }
    }

    public List<ToolSpec> specifications() {
        List<ToolSpec> specs = new ArrayList<>();
        for (AgentTool tool : tools.values()) {
            specs.add(tool.specification());
        }
        return specs;
    }

    public String execute(String toolName, String argumentsJson) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new DomainException("Ferramenta desconhecida: " + toolName);
        }
        return tool.execute(argumentsJson);
    }
}
