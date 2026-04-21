package com.anthonyrodriguez.papersage_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for Gemini embedding pipeline behavior.
 *
 * <p>These values tune throughput and resilience of document embedding:
 * concurrency, retry count, retry backoff, and completion timeout.</p>
 *
 * @param maxConcurrency           max in-flight embedding requests allowed at once
 * @param maxAttempts              max attempts per chunk (initial try + retries)
 * @param initialBackoffMillis     initial retry backoff in milliseconds
 * @param completionTimeoutSeconds max seconds to wait for embedding completion
 */
@ConfigurationProperties(prefix = "app.embedding")
public record GeminiEmbeddingProperties(
        int maxConcurrency,
        int maxAttempts,
        long initialBackoffMillis,
        long completionTimeoutSeconds
) {
}
