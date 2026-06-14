package com.triage.domain.valueobject;

/**
 * Categoria funcional do ticket, usada para roteamento.
 */
public enum Category {
    BILLING,
    TECHNICAL,
    ACCOUNT,
    FEEDBACK,
    OTHER;

    public static Category fromString(String raw) {
        if (raw == null) {
            return OTHER;
        }
        try {
            return Category.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OTHER;
        }
    }
}
