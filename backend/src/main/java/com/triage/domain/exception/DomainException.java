package com.triage.domain.exception;

/**
 * Exceção lançada quando uma regra de negócio do domínio é violada.
 * Mantém o domínio livre de dependências de frameworks de erro externos.
 */
public class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
