package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemanticRetrievalServiceTest {

    @Mock
    private GeminiEmbeddingService embeddingService;

    @InjectMocks
    private SemanticRetrievalService service;

    // 768-dimension zero vector — used as a neutral embedding in arrange sections
    private static final float[] ZERO_VECTOR = new float[768];

    // ─── Helper factories ─────────────────────────────────────────────────────

    private TextChunk makeChunk(int index, String text) {
        return new TextChunk("chunk-id-" + index, text, index, null);
    }

    /**
     * Returns a float[] where every element equals the given value.
     * Used to produce vectors with predictable cosine similarity.
     */
    private float[] uniformVector(float value) {
        float[] v = new float[768];
        for (int i = 0; i < v.length; i++) {
            v[i] = value;
        }
        return v;
    }

    @BeforeEach
    void setUp() {
        // Ensure a clean state before each test (InjectMocks recreates the service,
        // but the internal embeddedChunks list is also fresh on each new instance).
    }

    // ─── retrieveTopChunks — no chunks indexed ────────────────────────────────

    @Test
    void should_returnEmptyList_when_noChunksIndexed() {
        // Arrange — nothing indexed yet; no mock setup needed

        // Act
        List<RetrievalResult> result = service.retrieveTopChunks("What is the contribution?");

        // Assert
        assertThat(result).isEmpty();
    }

    // ─── getIndexedChunkCount — before and after indexing ────────────────────

    @Test
    void should_returnZero_when_noChunksHaveBeenIndexed() {
        // Act
        int count = service.getIndexedChunkCount();

        // Assert
        assertThat(count).isZero();
    }

    @Test
    void should_returnCorrectCount_when_chunksAreIndexed() {
        // Arrange
        List<TextChunk> chunks = List.of(makeChunk(0, "Alpha."), makeChunk(1, "Beta."), makeChunk(2, "Gamma."));
        when(embeddingService.embedDocuments(anyList(), isNull()))
                .thenReturn(List.of(ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR));

        // Act
        service.indexChunks(chunks);

        // Assert
        assertThat(service.getIndexedChunkCount()).isEqualTo(3);
    }

    // ─── indexChunks — replaces previous index ────────────────────────────────

    @Test
    void should_replaceOldChunks_when_indexChunksCalledASecondTime() {
        // Arrange — first upload with 3 chunks
        List<TextChunk> firstBatch = List.of(makeChunk(0, "A."), makeChunk(1, "B."), makeChunk(2, "C."));
        when(embeddingService.embedDocuments(anyList(), isNull()))
                .thenReturn(List.of(ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR))
                .thenReturn(List.of(ZERO_VECTOR));

        service.indexChunks(firstBatch);
        assertThat(service.getIndexedChunkCount()).isEqualTo(3);

        // Act — second upload with only 1 chunk
        List<TextChunk> secondBatch = List.of(makeChunk(0, "Only chunk."));
        service.indexChunks(secondBatch);

        // Assert — old chunks are gone, only the new one remains
        assertThat(service.getIndexedChunkCount()).isEqualTo(1);
    }

    // ─── retrieveTopChunks — returns at most 5 results ───────────────────────

    @Test
    void should_returnAtMostFiveChunks_when_moreChunksAreIndexed() {
        // Arrange — index 8 chunks
        List<TextChunk> chunks = List.of(
                makeChunk(0, "A."), makeChunk(1, "B."), makeChunk(2, "C."),
                makeChunk(3, "D."), makeChunk(4, "E."), makeChunk(5, "F."),
                makeChunk(6, "G."), makeChunk(7, "H.")
        );
        List<float[]> embeddings = List.of(
                ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR,
                ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR, ZERO_VECTOR
        );
        when(embeddingService.embedDocuments(anyList(), isNull())).thenReturn(embeddings);
        when(embeddingService.embedQuery(any())).thenReturn(ZERO_VECTOR);

        service.indexChunks(chunks);

        // Act
        List<RetrievalResult> result = service.retrieveTopChunks("Any question");

        // Assert
        assertThat(result).hasSizeLessThanOrEqualTo(5);
    }

    // ─── retrieveTopChunks — results are ranked by descending score ───────────

    @Test
    void should_rankResultsByDescendingScore_when_chunksHaveDifferentSimilarity() {
        // Arrange — three chunks with embeddings of different magnitudes.
        // Query vector = uniformVector(1.0f).
        // Cosine similarity depends on direction: all uniform vectors point in the same
        // direction, so we vary one dimension to differentiate scores.
        float[] highSim  = uniformVector(1.0f);   // cosine sim to query ≈ 1.0
        float[] medSim   = new float[768];
        medSim[0] = 1.0f;                          // only one non-zero dim → lower sim
        float[] lowSim   = new float[768];
        lowSim[767] = 0.1f;                        // different dim, tiny magnitude

        List<TextChunk> chunks = List.of(
                makeChunk(0, "Low relevance."),
                makeChunk(1, "High relevance."),
                makeChunk(2, "Medium relevance.")
        );
        when(embeddingService.embedDocuments(anyList(), isNull()))
                .thenReturn(List.of(lowSim, highSim, medSim));
        when(embeddingService.embedQuery(any())).thenReturn(uniformVector(1.0f));

        service.indexChunks(chunks);

        // Act
        List<RetrievalResult> result = service.retrieveTopChunks("What is the main result?");

        // Assert — first result must have the highest score
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).similarityScore())
                .isGreaterThanOrEqualTo(result.get(result.size() - 1).similarityScore());
    }

    // ─── retrieveTopChunks — delegates to embeddingService ───────────────────

    @Test
    void should_callEmbedQuery_when_retrieveTopChunksIsInvoked() {
        // Arrange
        List<TextChunk> chunks = List.of(makeChunk(0, "Some text."));
        when(embeddingService.embedDocuments(anyList(), isNull())).thenReturn(List.of(ZERO_VECTOR));
        when(embeddingService.embedQuery(any())).thenReturn(ZERO_VECTOR);
        service.indexChunks(chunks);

        String question = "What dataset was used?";

        // Act
        service.retrieveTopChunks(question);

        // Assert
        verify(embeddingService).embedQuery(eq(question));
    }
}
