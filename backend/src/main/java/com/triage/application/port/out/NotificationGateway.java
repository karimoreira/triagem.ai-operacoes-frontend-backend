package com.triage.application.port.out;

public interface NotificationGateway {
    void notifyEscalation(String ticketId, String summary);
}
