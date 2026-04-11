package com.anthonyrodriguez.papersage_backend.service;

import com.google.genai.Client;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

/**
 * Generates text embeddings using the Gemini Embedding API.
 *
 * <p>Uses the {@code text-embedding-004} model (768 dimensions) with task-type
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

    private final Client client;

    public GeminiEmbeddingService(Client client) {
        this.client = client;
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
        logger.info("Generating embeddings for {} document chunk(s) using virtual threads", total);

        // Pre-allocate result list with null slots to preserve insertion order
        List<float[]> embeddings = new ArrayList<>(Collections.nCopies(total, null));
        AtomicInteger completedCount = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>(total);

            for (int i = 0; i < total; i++) {
                final int index = i;
                final String text = texts.get(i);

                futures.add(executor.submit(() -> {
                    float[] embedding = embed(text, "RETRIEVAL_DOCUMENT");
                    embeddings.set(index, embedding);

                    if (index == 0) {
                        logger.info("First embedding generated — dimensions: {}", embedding.length);
                    }

                    int completed = completedCount.incrementAndGet();
                    if (onChunkEmbedded != null) {
                        onChunkEmbedded.accept(completed, total);
                    }
                }));
            }

            // Wait for all virtual threads to finish
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Embedding interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Embedding failed for one or more chunks", e.getCause());
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
                .orElseThrow(() -> new RuntimeException("Gemini embedding response contained no embeddings"));

        List<Float> values = embeddingsList.get(0).values()
                .orElseThrow(() -> new RuntimeException("Gemini embedding contained no values"));

        float[] vector = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            vector[i] = values.get(i);
        }
        return vector;
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
