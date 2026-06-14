package com.triage.application.port.out;

public interface TaskManagerGateway {
    String createTask(String title, String description, String priority);
}
