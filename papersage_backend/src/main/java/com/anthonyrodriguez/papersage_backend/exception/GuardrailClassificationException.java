package com.anthonyrodriguez.papersage_backend.exception;

/**
 * Thrown when AI-based CS-paper guardrail classification cannot be completed.
 */
public class GuardrailClassificationException extends RuntimeException {

    public GuardrailClassificationException(String message) {
        super(message);
    }

    public GuardrailClassificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
