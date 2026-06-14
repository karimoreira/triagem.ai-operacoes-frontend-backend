package com.triage.application.port.out;

import java.util.List;

public interface LLMProvider {
    LLMResponse chat(List<LLMMessage> messages, List<ToolSpec> tools);

    record LLMMessage(Role role, String content) {
        public enum Role { SYSTEM, USER, ASSISTANT, TOOL }
    }

    record LLMResponse(String text, List<ToolCall> toolCalls) {
        public boolean requiresToolExecution() {
            return toolCalls != null && !toolCalls.isEmpty();
        }
    }

    record ToolCall(String id, String name, String argumentsJson) {}

    record ToolSpec(String name, String description, String jsonSchema) {}
}
