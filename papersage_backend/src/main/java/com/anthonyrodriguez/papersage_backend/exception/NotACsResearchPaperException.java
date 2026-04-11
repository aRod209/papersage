package com.anthonyrodriguez.papersage_backend.exception;

/**
 * Thrown when the guardrail classifier determines that the uploaded document
 * is not a computer science research paper.
 *
 * <p>This is an unchecked exception handled by {@link GlobalExceptionHandler}
 * which maps it to HTTP 422 Unprocessable Entity.</p>
 */
public class NotACsResearchPaperException extends RuntimeException {

    public NotACsResearchPaperException(String reason) {
        super(reason);
    }
}
