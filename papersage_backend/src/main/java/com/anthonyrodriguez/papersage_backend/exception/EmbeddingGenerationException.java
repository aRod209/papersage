package com.anthonyrodriguez.papersage_backend.exception;

/**
 * Thrown when Gemini embedding generation fails for document chunks or query text.
 */
public class EmbeddingGenerationException extends RuntimeException {

    public EmbeddingGenerationException(String message) {
        super(message);
    }

    public EmbeddingGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
