package com.anthonyrodriguez.papersage_backend.exception;

/**
 * Thrown when grounded answer generation fails.
 */
public class GroundedAnswerGenerationException extends RuntimeException {

    public GroundedAnswerGenerationException(String message) {
        super(message);
    }

    public GroundedAnswerGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
