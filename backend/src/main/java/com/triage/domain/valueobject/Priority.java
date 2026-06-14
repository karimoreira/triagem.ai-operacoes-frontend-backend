package com.triage.domain.valueobject;

/**
 * Prioridade de um ticket. Modelada como enum para garantir um conjunto
 * fechado de valores válidos (impossível representar um estado inválido).
 */
public enum Priority {
    LOW(1),
    MEDIUM(2),
    HIGH(3),
    CRITICAL(4);

    private final int weight;

    Priority(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public boolean isAtLeast(Priority other) {
        return this.weight >= other.weight;
    }

    public static Priority fromString(String raw) {
        if (raw == null) {
            return MEDIUM;
        }
        try {
            return Priority.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
