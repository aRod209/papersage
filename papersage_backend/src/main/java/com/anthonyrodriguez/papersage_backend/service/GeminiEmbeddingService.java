package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.config.GeminiEmbeddingProperties;
import com.anthonyrodriguez.papersage_backend.exception.EmbeddingGenerationException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Generates text embeddings using the Gemini Embedding API.
 *
 * <p>Uses the {@code gemini-embedding-001} model (3072 dimensions) with task-type
 * hints to optimize vectors for retrieval:</p>
 * <ul>
 *   <li>{@code RETRIEVAL_DOCUMENT} — for paper chunk embeddings (stored)</li>
 *   <li>{@code RETRIEVAL_QUERY} — for user question embeddings (transient)</li>
 * </ul>
 *
 * <p>Document embeddings are generated in parallel using Java 21 virtual threads,
 * significantly reducing total embedding time for large papers.
 * Storage and similarity search are handled by {@link SemanticRetrievalService}.</p>
 */
@Service
public class GeminiEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiEmbeddingService.class);
    private static final String EMBEDDING_MODEL = "gemini-embedding-001";
    private static final long MAX_BACKOFF_MILLIS = 8_000L;

    private final Client client;
    private final int maxConcurrency;
    private final int maxAttempts;
    private final long initialBackoffMillis;
    private final long completionTimeoutSeconds;

    public GeminiEmbeddingService(Client client, GeminiEmbeddingProperties embeddingProperties) {
        this.client = client;
        this.maxConcurrency = Math.max(1, embeddingProperties.maxConcurrency());
        this.maxAttempts = Math.max(1, embeddingProperties.maxAttempts());
        this.initialBackoffMillis = Math.max(0, embeddingProperties.initialBackoffMillis());
        this.completionTimeoutSeconds = Math.max(1, embeddingProperties.completionTimeoutSeconds());
        logger.info("Gemini embedding client initialized with model: {}", EMBEDDING_MODEL);
    }

    /**
     * Generates embeddings for a batch of document texts in parallel using virtual threads.
     * Uses the {@code RETRIEVAL_DOCUMENT} task type to optimize for retrieval indexing.
     *
     * @param texts the list of text strings to embed
     * @return a list of embedding vectors in the same order as the input texts
     */
    public List<float[]> embedDocuments(List<String> texts) {
        return embedDocuments(texts, null);
    }

    /**
     * Generates embeddings for a batch of document texts in parallel, notifying a
     * callback after each chunk completes.
     *
     * <p>Uses a virtual-thread-per-task executor (Java 21) so that each blocking
     * Gemini API call runs on its own lightweight virtual thread, dramatically
     * reducing total wall-clock time compared to sequential embedding.</p>
     *
     * <p>The {@code onChunkEmbedded} callback receives {@code (completedIndex, totalCount)} after
     * each chunk is embedded, allowing callers to report per-chunk progress.</p>
     *
     * @param texts           the list of text strings to embed
     * @param onChunkEmbedded optional callback invoked after each chunk with (completedCount, totalCount);
     *                        pass {@code null} to skip progress reporting
     * @return a list of embedding vectors in the same order as the input texts
     */
    public List<float[]> embedDocuments(List<String> texts, BiConsumer<Integer, Integer> onChunkEmbedded) {
        int total = texts.size();
        logger.info("Generating embeddings for {} document chunk(s) using virtual threads (maxConcurrency={}, maxAttempts={}, timeout={}s)",
                total, maxConcurrency, maxAttempts, completionTimeoutSeconds);

        if (total == 0) {
            return List.of();
        }

        // Pre-allocate result list with null slots to preserve insertion order
        List<float[]> embeddings = new ArrayList<>(Collections.nCopies(total, null));
        AtomicInteger completedCount = new AtomicInteger(0);
        Semaphore concurrencyGate = new Semaphore(maxConcurrency);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletionService<ChunkEmbeddingResult> completionService = new ExecutorCompletionService<>(executor);
            List<Future<?>> futures = new ArrayList<>(total);

            for (int i = 0; i < total; i++) {
                final int index = i;
                final String text = texts.get(i);

                futures.add(completionService.submit(() -> {
                    concurrencyGate.acquire();
                    try {
                        float[] embedding = embedWithRetry(text, "RETRIEVAL_DOCUMENT", index, total);
                        return new ChunkEmbeddingResult(index, embedding);
                    } finally {
                        concurrencyGate.release();
                    }
                }));
            }

            for (int i = 0; i < total; i++) {
                Future<ChunkEmbeddingResult> finished = completionService.poll(completionTimeoutSeconds, TimeUnit.SECONDS);
                if (finished == null) {
                    cancelAll(futures);
                    throw new EmbeddingGenerationException(
                            "Timed out waiting for embedding completion after %d seconds".formatted(completionTimeoutSeconds)
                    );
                }

                ChunkEmbeddingResult result = finished.get();
                embeddings.set(result.index(), result.embedding());

                if (result.index() == 0) {
                    logger.info("First embedding generated — dimensions: {}", result.embedding().length);
                }

                int completed = completedCount.incrementAndGet();
                if (onChunkEmbedded != null) {
                    onChunkEmbedded.accept(completed, total);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EmbeddingGenerationException("Embedding interrupted", e);
        } catch (ExecutionException e) {
            throw new EmbeddingGenerationException("Embedding failed for one or more chunks", e.getCause());
        }

        logger.info("All {} document embeddings generated successfully", embeddings.size());
        return embeddings;
    }

    /**
     * Generates an embedding for a single user query.
     * Uses the {@code RETRIEVAL_QUERY} task type to optimize for retrieval matching.
     *
     * @param query the user's question
     * @return the embedding vector for the query
     */
    public float[] embedQuery(String query) {
        logger.info("Generating embedding for query: \"{}\"", truncate(query, 100));
        float[] embedding = embed(query, "RETRIEVAL_QUERY");
        logger.info("Query embedding generated — dimensions: {}", embedding.length);
        return embedding;
    }

    /**
     * Calls the Gemini embed API for a single text with the specified task type.
     */
    private float[] embed(String text, String taskType) {
        EmbedContentConfig config = EmbedContentConfig.builder()
                .taskType(taskType)
                .build();

        EmbedContentResponse response = client.models.embedContent(EMBEDDING_MODEL, text, config);

        List<ContentEmbedding> embeddingsList = response.embeddings()
                .orElseThrow(() -> new EmbeddingGenerationException("Gemini embedding response contained no embeddings"));

        List<Float> values = embeddingsList.get(0).values()
                .orElseThrow(() -> new EmbeddingGenerationException("Gemini embedding contained no values"));

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        return vector;
    }

    private float[] embedWithRetry(String text, String taskType, int chunkIndex, int totalChunks) {
        long backoffMillis = initialBackoffMillis;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                logger.debug("Embedding chunk {}/{} (attempt {}/{})", chunkIndex + 1, totalChunks, attempt, maxAttempts);
                return embed(text, taskType);
            } catch (RuntimeException ex) {
                boolean retryable = isRetryable(ex);
                if (!retryable || attempt == maxAttempts) {
                    throw new EmbeddingGenerationException(
                            "Failed to embed chunk %d/%d after %d attempt(s)".formatted(chunkIndex + 1, totalChunks, attempt),
                            ex
                    );
                }

                long jitteredBackoffMillis = computeJitteredBackoffMillis(backoffMillis);

                logger.warn("Transient embedding failure on chunk {}/{} (attempt {}/{}). Retrying in {} ms (base backoff={} ms). Cause: {}",
                        chunkIndex + 1,
                        totalChunks,
                        attempt,
                        maxAttempts,
                        jitteredBackoffMillis,
                        backoffMillis,
                        ex.getMessage());

                try {
                    Thread.sleep(jitteredBackoffMillis);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new EmbeddingGenerationException("Embedding retry backoff interrupted", interruptedException);
                }

                backoffMillis = Math.min(backoffMillis * 2, MAX_BACKOFF_MILLIS);
            }
        }

        throw new EmbeddingGenerationException("Embedding failed unexpectedly");
    }

    private static long computeJitteredBackoffMillis(long baseBackoffMillis) {
        if (baseBackoffMillis <= 1L) {
            return baseBackoffMillis;
        }

        long minBackoffMillis = Math.max(1L, baseBackoffMillis / 2L);
        return ThreadLocalRandom.current().nextLong(minBackoffMillis, baseBackoffMillis + 1L);
    }

    private static boolean isRetryable(RuntimeException ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        if (root instanceof ApiException || root instanceof IOException) {
            return true;
        }

        String message = root.getMessage();
        if (message == null) {
            return false;
        }

        return message.contains("429") || message.contains("503") || message.toLowerCase().contains("timeout");
    }

    private static void cancelAll(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            future.cancel(true);
        }
    }

    private record ChunkEmbeddingResult(int index, float[] embedding) {
    }

    /**
     * Truncates a string for safe logging output.
     */
    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
