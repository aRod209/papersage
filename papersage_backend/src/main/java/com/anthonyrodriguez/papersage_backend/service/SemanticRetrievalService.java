package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.EmbeddedChunk;
import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Orchestrates in-memory semantic retrieval over paper chunks.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Accepts chunks from the upload pipeline and generates + stores embeddings</li>
 *   <li>Accepts a user question, embeds it, and finds the top-K most similar chunks</li>
 * </ul>
 *
 * <p>Similarity is computed via cosine similarity — no external vector database needed.</p>
 */
@Service
public class SemanticRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(SemanticRetrievalService.class);
    private static final int DEFAULT_TOP_K = 5;

    private final GeminiEmbeddingService embeddingService;
    private final List<EmbeddedChunk> embeddedChunks = new ArrayList<>();

    public SemanticRetrievalService(GeminiEmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    /**
     * Embeds all provided chunks and stores them in memory for later retrieval.
     * Replaces any previously indexed chunks.
     *
     * @param chunks the text chunks produced by {@link TextChunkingService}
     */
    public void indexChunks(List<TextChunk> chunks) {
        indexChunks(chunks, null);
    }

    /**
     * Embeds all provided chunks and stores them in memory, reporting per-chunk progress.
     *
     * <p>The {@code onChunkEmbedded} callback is invoked after each chunk is embedded
     * with {@code (completedCount, totalCount)}, enabling callers to stream progress events.</p>
     *
     * @param chunks          the text chunks produced by {@link TextChunkingService}
     * @param onChunkEmbedded optional callback invoked after each chunk; pass {@code null} to skip
     */
    public void indexChunks(List<TextChunk> chunks, BiConsumer<Integer, Integer> onChunkEmbedded) {
        logger.info("Indexing {} chunk(s) — generating embeddings...", chunks.size());

        List<String> texts = chunks.stream()
                .map(TextChunk::chunkText)
                .toList();

        List<float[]> embeddings = embeddingService.embedDocuments(texts, onChunkEmbedded);

        embeddedChunks.clear();
        for (int i = 0; i < chunks.size(); i++) {
            embeddedChunks.add(new EmbeddedChunk(chunks.get(i), embeddings.get(i)));
        }

        logger.info("Indexing complete — {} chunk(s) embedded and stored in memory", embeddedChunks.size());
    }

    /**
     * Retrieves the top-K most semantically similar chunks for a given question.
     *
     * @param question the user's natural-language question
     * @return ranked list of the top 5 matching chunks with similarity scores
     */
    public List<RetrievalResult> retrieveTopChunks(String question) {
        return retrieveTopChunks(question, DEFAULT_TOP_K);
    }

    /**
     * Retrieves the top-K most semantically similar chunks for a given question.
     *
     * @param question the user's natural-language question
     * @param topK     the number of top results to return
     * @return ranked list of matching chunks with similarity scores (descending)
     */
    public List<RetrievalResult> retrieveTopChunks(String question, int topK) {
        if (embeddedChunks.isEmpty()) {
            logger.warn("Retrieval attempted but no chunks are indexed. Upload a paper first.");
            return List.of();
        }

        logger.info("Retrieving top {} chunk(s) for question: \"{}\"", topK, truncate(question, 100));

        float[] queryEmbedding = embeddingService.embedQuery(question);

        List<RetrievalResult> scoredResults = new ArrayList<>(embeddedChunks.size());
        for (EmbeddedChunk ec : embeddedChunks) {
            double score = cosineSimilarity(queryEmbedding, ec.embedding());
            scoredResults.add(new RetrievalResult(ec.chunk(), score));
        }

        scoredResults.sort(Comparator.comparingDouble(RetrievalResult::similarityScore).reversed());

        logRetrievalResults(scoredResults, topK);

        return scoredResults.stream()
                .limit(topK)
                .toList();
    }

    /**
     * Returns the count of currently indexed chunks.
     */
    public int getIndexedChunkCount() {
        return embeddedChunks.size();
    }

    /**
     * Computes cosine similarity between two vectors.
     *
     * @return a value between -1.0 and 1.0 (higher = more similar)
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0;
        }
        return dotProduct / denominator;
    }

    /**
     * Logs all chunk similarity scores and highlights the top-K results.
     */
    private void logRetrievalResults(List<RetrievalResult> sortedResults, int topK) {
        logger.info("=== Retrieval ranking (all {} chunks) ===", sortedResults.size());
        for (int i = 0; i < sortedResults.size(); i++) {
            RetrievalResult result = sortedResults.get(i);
            String marker = (i < topK) ? " ★ TOP" : "";
            logger.info("  Rank {}: chunkIndex={}, section={}, score={}, preview=\"{}\"{}",
                    i + 1,
                    result.chunk().chunkIndex(),
                    result.chunk().sectionLabel(),
                    String.format("%.4f", result.similarityScore()),
                    truncate(result.chunk().chunkText(), 80),
                    marker);
        }
        logger.info("=== Returning top {} chunk(s) ===", Math.min(topK, sortedResults.size()));
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
