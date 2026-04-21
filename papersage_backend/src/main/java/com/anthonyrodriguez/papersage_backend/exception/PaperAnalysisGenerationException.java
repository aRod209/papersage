package com.anthonyrodriguez.papersage_backend.exception;

/**
 * Thrown when structured paper analysis generation fails.
 */
public class PaperAnalysisGenerationException extends RuntimeException {

    public PaperAnalysisGenerationException(String message) {
        super(message);
    }

    public PaperAnalysisGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
