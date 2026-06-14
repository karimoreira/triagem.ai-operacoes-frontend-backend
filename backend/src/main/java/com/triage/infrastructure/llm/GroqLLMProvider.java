package com.triage.infrastructure.llm;

import com.triage.application.port.out.LLMProvider;
import com.triage.infrastructure.config.Json;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter do provedor de LLM usando a API da Groq (compatível com o formato
 * OpenAI Chat Completions), que oferece tier gratuito e tool calling.
 *
 * Configurado via variável de ambiente GROQ_API_KEY. Se a chave não estiver
 * presente, cai automaticamente em um modo simulado (offline), permitindo
 * rodar o projeto sem credenciais — útil para portfólio e testes locais.
 */
public final class GroqLLMProvider implements LLMProvider {

    private static final String ENDPOINT = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";

    private final HttpClient httpClient;
    private final String apiKey;

    public GroqLLMProvider() {
        this.apiKey = System.getenv("GROQ_API_KEY");
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    }

    @Override
    public LLMResponse chat(List<LLMMessage> messages, List<ToolSpec> tools) {
        if (apiKey == null || apiKey.isBlank()) {
            return simulate(messages);
        }
        try {
            return callGroq(messages, tools);
        } catch (Exception ex) {
            // Falha de rede/credencial não deve derrubar a demo: cai no modo simulado.
            return simulate(messages);
        }
    }

    private LLMResponse callGroq(List<LLMMessage> messages, List<ToolSpec> tools) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", MODEL);
        payload.put("temperature", 0.2);

        List<Object> apiMessages = new ArrayList<>();
        for (LLMMessage m : messages) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("role", mapRole(m.role()));
            entry.put("content", m.content());
            apiMessages.add(entry);
        }
        payload.put("messages", apiMessages);

        if (tools != null && !tools.isEmpty()) {
            List<Object> apiTools = new ArrayList<>();
            for (ToolSpec spec : tools) {
                Map<String, Object> fn = new LinkedHashMap<>();
                fn.put("name", spec.name());
                fn.put("description", spec.description());
                fn.put("parameters", Json.parse(spec.jsonSchema()));
                Map<String, Object> tool = new LinkedHashMap<>();
                tool.put("type", "function");
                tool.put("function", fn);
                apiTools.add(tool);
            }
            payload.put("tools", apiTools);
        }

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(ENDPOINT))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(Json.write(payload)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return parseResponse(response.body());
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseResponse(String body) {
        Map<String, Object> root = Json.toMap(body);
        List<Object> choices = (List<Object>) root.get("choices");
        if (choices == null || choices.isEmpty()) {
            return new LLMResponse("{}", List.of());
        }
        Map<String, Object> message = (Map<String, Object>) ((Map<String, Object>) choices.get(0)).get("message");

        List<ToolCall> toolCalls = new ArrayList<>();
        Object rawToolCalls = message.get("tool_calls");
        if (rawToolCalls instanceof List<?> list) {
            for (Object item : list) {
                Map<String, Object> tc = (Map<String, Object>) item;
                Map<String, Object> fn = (Map<String, Object>) tc.get("function");
                toolCalls.add(new ToolCall(
                    String.valueOf(tc.get("id")),
                    String.valueOf(fn.get("name")),
                    String.valueOf(fn.get("arguments"))));
            }
        }

        String content = message.get("content") == null ? "" : String.valueOf(message.get("content"));
        return new LLMResponse(content, toolCalls);
    }

    private String mapRole(LLMMessage.Role role) {
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> "tool";
        };
    }

    /**
     * Modo simulado: heurística simples para rodar sem API key. Demonstra
     * o fluxo do agente de ponta a ponta de forma determinística.
     */
    private LLMResponse simulate(List<LLMMessage> messages) {
        String lastUser = messages.stream()
            .filter(m -> m.role() == LLMMessage.Role.USER)
            .reduce((a, b) -> b)
            .map(LLMMessage::content)
            .orElse("")
            .toLowerCase();

        String priority = "MEDIUM";
        String category = "OTHER";
        boolean escalate = false;

        if (lastUser.contains("cobran") || lastUser.contains("fatura") || lastUser.contains("pagamento")) {
            category = "BILLING";
        } else if (lastUser.contains("erro") || lastUser.contains("bug") || lastUser.contains("falha")) {
            category = "TECHNICAL";
            priority = "HIGH";
        } else if (lastUser.contains("senha") || lastUser.contains("login") || lastUser.contains("conta")) {
            category = "ACCOUNT";
        }

        if (lastUser.contains("urgente") || lastUser.contains("crítico") || lastUser.contains("parou")) {
            priority = "CRITICAL";
            escalate = true;
        }

        Map<String, Object> decision = new LinkedHashMap<>();
        decision.put("priority", priority);
        decision.put("category", category);
        decision.put("shouldEscalate", escalate);
        decision.put("suggestedReply", "Recebemos seu chamado e já estamos analisando.");
        decision.put("reasoning", "[modo simulado] Classificado por heurística de palavras-chave.");

        return new LLMResponse(Json.write(decision), List.of());
    }
}
