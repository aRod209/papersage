package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.config.GeminiEmbeddingProperties;
import com.anthonyrodriguez.papersage_backend.exception.EmbeddingGenerationException;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.ContentEmbedding;
import com.google.genai.types.EmbedContentConfig;
import com.google.genai.types.EmbedContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiEmbeddingServiceTest {

    private static final GeminiEmbeddingProperties DEFAULT_PROPERTIES =
            new GeminiEmbeddingProperties(6, 3, 500L, 30L);

    @Mock
    private Client mockClient;

    @Mock
    private Models mockModels;

    private GeminiEmbeddingService service;

    @BeforeEach
    void setUp() {
        // Client.models is a final field — use ReflectionTestUtils to inject the mock
        ReflectionTestUtils.setField(mockClient, "models", mockModels);
        service = new GeminiEmbeddingService(mockClient, DEFAULT_PROPERTIES);
    }

    private GeminiEmbeddingService configuredService(int maxConcurrency,
                                                     int maxAttempts,
                                                     long initialBackoffMillis,
                                                     long completionTimeoutSeconds) {
        GeminiEmbeddingProperties properties = new GeminiEmbeddingProperties(
                maxConcurrency,
                maxAttempts,
                initialBackoffMillis,
                completionTimeoutSeconds
        );
        return new GeminiEmbeddingService(mockClient, properties);
    }

    // ─── Helper: builds a mock EmbedContentResponse with a given vector ───────

    private EmbedContentResponse buildEmbedResponse(float... values) {
        List<Float> floatList = new java.util.ArrayList<>();
        for (float v : values) {
            floatList.add(v);
        }

        ContentEmbedding embedding = mock(ContentEmbedding.class);
        when(embedding.values()).thenReturn(Optional.of(floatList));

        EmbedContentResponse response = mock(EmbedContentResponse.class);
        when(response.embeddings()).thenReturn(Optional.of(List.of(embedding)));

        return response;
    }

    // ─── embedQuery ───────────────────────────────────────────────────────────

    @Test
    void should_returnEmbeddingVector_when_embedQueryIsCalled() {
        // Arrange
        EmbedContentResponse response = buildEmbedResponse(0.1f, 0.2f, 0.3f);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        // Act
        float[] result = service.embedQuery("What is the main contribution?");

        // Assert
        assertThat(result).hasSize(3);
        assertThat(result[0]).isEqualTo(0.1f);
        assertThat(result[1]).isEqualTo(0.2f);
        assertThat(result[2]).isEqualTo(0.3f);
    }

    @Test
    void should_callEmbedContent_with_RETRIEVAL_QUERY_taskType_when_embedQueryIsCalled() {
        // Arrange
        EmbedContentResponse response = buildEmbedResponse(0.5f);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        ArgumentCaptor<EmbedContentConfig> configCaptor = ArgumentCaptor.forClass(EmbedContentConfig.class);

        // Act
        service.embedQuery("Some question");

        // Assert
        verify(mockModels).embedContent(anyString(), anyString(), configCaptor.capture());
        assertThat(configCaptor.getValue().taskType()).isPresent();
        assertThat(configCaptor.getValue().taskType().get()).isEqualTo("RETRIEVAL_QUERY");
    }

    // ─── embedDocuments ───────────────────────────────────────────────────────

    @Test
    void should_returnOneEmbeddingPerText_when_embedDocumentsIsCalled() {
        // Arrange
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(1, String.class);
                    return switch (text) {
                        case "Text one" -> buildEmbedResponse(0.1f, 0.2f);
                        case "Text two" -> buildEmbedResponse(0.3f, 0.4f);
                        default -> throw new IllegalArgumentException("Unexpected text: " + text);
                    };
                });

        // Act
        List<float[]> results = service.embedDocuments(List.of("Text one", "Text two"));

        // Assert
        assertThat(results).hasSize(2);
        assertThat(results.get(0)).containsExactly(0.1f, 0.2f);
        assertThat(results.get(1)).containsExactly(0.3f, 0.4f);
    }

    @Test
    void should_callEmbedContent_once_per_document_when_embedDocumentsIsCalled() {
        // Arrange
        EmbedContentResponse response = buildEmbedResponse(0.1f);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        // Act
        service.embedDocuments(List.of("Doc A", "Doc B", "Doc C"));

        // Assert
        verify(mockModels, times(3)).embedContent(anyString(), anyString(), any(EmbedContentConfig.class));
    }

    @Test
    void should_callEmbedContent_with_RETRIEVAL_DOCUMENT_taskType_when_embedDocumentsIsCalled() {
        // Arrange
        EmbedContentResponse response = buildEmbedResponse(0.5f);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        ArgumentCaptor<EmbedContentConfig> configCaptor = ArgumentCaptor.forClass(EmbedContentConfig.class);

        // Act
        service.embedDocuments(List.of("Some document text"));

        // Assert
        verify(mockModels).embedContent(anyString(), anyString(), configCaptor.capture());
        assertThat(configCaptor.getValue().taskType()).isPresent();
        assertThat(configCaptor.getValue().taskType().get()).isEqualTo("RETRIEVAL_DOCUMENT");
    }

    @Test
    void should_returnEmptyList_when_embedDocumentsCalledWithEmptyList() {
        // Act
        List<float[]> results = service.embedDocuments(List.of());

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    void should_retryTransientFailureAndEventuallySucceed_when_embedDocumentsIsCalled() {
        // Arrange
        GeminiEmbeddingService retryingService = configuredService(2, 3, 0, 30);
        AtomicInteger attempts = new AtomicInteger(0);

        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenAnswer(invocation -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new RuntimeException("429 Too Many Requests");
                    }
                    return buildEmbedResponse(0.9f, 0.8f);
                });

        // Act
        List<float[]> results = retryingService.embedDocuments(List.of("Doc A"));

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0)).containsExactly(0.9f, 0.8f);
        verify(mockModels, times(2)).embedContent(anyString(), anyString(), any(EmbedContentConfig.class));
    }

    @Test
    void should_computeJitteredBackoffWithinExpectedRange_when_backoffIsGreaterThanOne() {
        // Arrange
        long baseBackoffMillis = 500L;

        // Act + Assert
        for (int i = 0; i < 30; i++) {
            Long jitteredBackoffMillis = ReflectionTestUtils.invokeMethod(
                    GeminiEmbeddingService.class,
                    "computeJitteredBackoffMillis",
                    baseBackoffMillis
            );

            assertThat(jitteredBackoffMillis).isNotNull();
            assertThat(jitteredBackoffMillis)
                    .isGreaterThanOrEqualTo(baseBackoffMillis / 2)
                    .isLessThanOrEqualTo(baseBackoffMillis);
        }
    }

    @Test
    void should_notApplyJitter_when_backoffIsZeroOrOne() {
        // Act
        Long zeroBackoffResult = ReflectionTestUtils.invokeMethod(
                GeminiEmbeddingService.class,
                "computeJitteredBackoffMillis",
                0L
        );
        Long oneBackoffResult = ReflectionTestUtils.invokeMethod(
                GeminiEmbeddingService.class,
                "computeJitteredBackoffMillis",
                1L
        );

        // Assert
        assertThat(zeroBackoffResult).isNotNull().isZero();
        assertThat(oneBackoffResult).isNotNull().isEqualTo(1L);
    }

    @Test
    void should_failFastOnNonRetryableFailure_when_embedDocumentsIsCalled() {
        // Arrange
        GeminiEmbeddingService retryingService = configuredService(2, 3, 0, 30);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenThrow(new RuntimeException("400 bad request"));

        // Act & Assert
        assertThatThrownBy(() -> retryingService.embedDocuments(List.of("Doc A")))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Embedding failed for one or more chunks")
                .satisfies(ex -> assertThat(ex.getCause())
                        .isNotNull()
                        .hasMessage("Failed to embed chunk 1/1 after 1 attempt(s)"))
                .hasRootCauseMessage("400 bad request");

        verify(mockModels, times(1)).embedContent(anyString(), anyString(), any(EmbedContentConfig.class));
    }

    @Test
    void should_throwTimeout_when_embeddingTaskDoesNotCompleteWithinTimeout() {
        // Arrange
        GeminiEmbeddingService timeoutService = configuredService(1, 1, 0, 1);
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(2_000);
                    return buildEmbedResponse(0.1f);
                });

        // Act & Assert
        assertThatThrownBy(() -> timeoutService.embedDocuments(List.of("Doc A")))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("Timed out waiting for embedding completion after 1 seconds");
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    void should_throwEmbeddingGenerationException_when_embeddingsResponseContainsNoEmbeddings() {
        // Arrange
        EmbedContentResponse response = mock(EmbedContentResponse.class);
        when(response.embeddings()).thenReturn(Optional.empty());
        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> service.embedQuery("test"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("no embeddings");
    }

    @Test
    void should_throwEmbeddingGenerationException_when_embeddingContainsNoValues() {
        // Arrange
        ContentEmbedding embedding = mock(ContentEmbedding.class);
        when(embedding.values()).thenReturn(Optional.empty());

        EmbedContentResponse response = mock(EmbedContentResponse.class);
        when(response.embeddings()).thenReturn(Optional.of(List.of(embedding)));

        when(mockModels.embedContent(anyString(), anyString(), any(EmbedContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> service.embedQuery("test"))
                .isInstanceOf(EmbeddingGenerationException.class)
                .hasMessageContaining("no values");
    }
}
