package com.triage.infrastructure.integration;

import com.triage.application.port.out.NotificationGateway;

/**
 * Implementação de notificação que registra no console. Em produção,
 * seria substituída por um adapter Slack/Teams/e-mail, sem alterar o
 * domínio (mesma interface NotificationGateway).
 */
public final class ConsoleNotificationGateway implements NotificationGateway {

    @Override
    public void notifyEscalation(String ticketId, String summary) {
        System.out.println("[NOTIFICAÇÃO] Escalando ticket " + ticketId + " -> " + summary);
    }
}
