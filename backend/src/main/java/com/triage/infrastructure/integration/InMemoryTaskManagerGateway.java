package com.triage.infrastructure.integration;

import com.triage.application.port.out.TaskManagerGateway;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementação simulada de um gerenciador de tarefas (estilo Jira).
 * Gera ids sequenciais e registra no console. Substituível por um
 * adapter real via API sem impacto no domínio.
 */
public final class InMemoryTaskManagerGateway implements TaskManagerGateway {

    private final AtomicInteger counter = new AtomicInteger(1000);

    @Override
    public String createTask(String title, String description, String priority) {
        String taskId = "TASK-" + counter.incrementAndGet();
        System.out.println("[TAREFA] " + taskId + " [" + priority + "] " + title + " :: " + description);
        return taskId;
    }
}
