package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.AnswerResponse;
import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import com.google.genai.Client;
import com.google.genai.Models;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroundedAnswerServiceTest {

    @Mock
    private SemanticRetrievalService retrievalService;

    @Mock
    private Client mockClient;

    @Mock
    private Models mockModels;

    private GroundedAnswerService service;

    private static final String PROMPT_TEMPLATE = "Context:\n{chunks}\n\nQuestion: {question}";

    @BeforeEach
    void setUp() throws IOException {
        // Client.models is a final field — use ReflectionTestUtils to inject the mock
        ReflectionTestUtils.setField(mockClient, "models", mockModels);
        Resource promptResource = new ByteArrayResource(PROMPT_TEMPLATE.getBytes());
        service = new GroundedAnswerService(retrievalService, mockClient, promptResource);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private RetrievalResult makeRetrievalResult(int index, String text, double score) {
        TextChunk chunk = new TextChunk("chunk-id-" + index, text, index, "INTRODUCTION");
        return new RetrievalResult(chunk, score);
    }

    // ─── No paper uploaded (empty retrieval) ─────────────────────────────────

    @Test
    void should_returnNoPaperMessage_when_noChunksAvailable() {
        // Arrange
        when(retrievalService.retrieveTopChunks(any())).thenReturn(List.of());

        // Act
        AnswerResponse response = service.answerQuestion("What is this paper about?");

        // Assert
        assertThat(response.question()).isEqualTo("What is this paper about?");
        assertThat(response.answer()).contains("No paper has been uploaded");
        assertThat(response.sources()).isEmpty();
    }

    // ─── Happy path — delegates retrieval and calls Gemini ───────────────────

    @Test
    void should_callRetrievalService_with_theGivenQuestion() {
        // Arrange
        String question = "What are the key contributions?";
        List<RetrievalResult> chunks = List.of(makeRetrievalResult(0, "Some context text.", 0.9));

        when(retrievalService.retrieveTopChunks(question)).thenReturn(chunks);

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("The key contributions are...");
        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(mockResponse);

        // Act
        service.answerQuestion(question);

        // Assert
        verify(retrievalService).retrieveTopChunks(eq(question));
    }

    @Test
    void should_returnAnswerAndSources_when_chunksAreAvailable() {
        // Arrange
        String question = "What dataset was used?";
        List<RetrievalResult> chunks = List.of(
                makeRetrievalResult(0, "We used the ImageNet dataset.", 0.95),
                makeRetrievalResult(1, "Experiments were conducted on ImageNet.", 0.88)
        );

        when(retrievalService.retrieveTopChunks(question)).thenReturn(chunks);

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("The paper used the ImageNet dataset.");
        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(mockResponse);

        // Act
        AnswerResponse response = service.answerQuestion(question);

        // Assert
        assertThat(response.question()).isEqualTo(question);
        assertThat(response.answer()).isEqualTo("The paper used the ImageNet dataset.");
        assertThat(response.sources()).hasSize(2);
        assertThat(response.sources().get(0).chunkId()).isEqualTo("chunk-id-0");
        assertThat(response.sources().get(0).similarityScore()).isEqualTo(0.95);
    }

    // ─── Source references contain correct metadata ───────────────────────────

    @Test
    void should_mapSourceReferenceFields_correctly_from_retrievalResult() {
        // Arrange
        String question = "What is the accuracy?";
        TextChunk chunk = new TextChunk("uuid-abc", "Accuracy was 94%.", 3, "RESULTS");
        RetrievalResult result = new RetrievalResult(chunk, 0.87);

        when(retrievalService.retrieveTopChunks(question)).thenReturn(List.of(result));

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("The accuracy was 94%.");
        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(mockResponse);

        // Act
        AnswerResponse response = service.answerQuestion(question);

        // Assert
        assertThat(response.sources()).hasSize(1);
        assertThat(response.sources().get(0).chunkId()).isEqualTo("uuid-abc");
        assertThat(response.sources().get(0).chunkIndex()).isEqualTo(3);
        assertThat(response.sources().get(0).sectionLabel()).isEqualTo("RESULTS");
        assertThat(response.sources().get(0).similarityScore()).isEqualTo(0.87);
    }

    // ─── Gemini returns blank response ───────────────────────────────────────

    @Test
    void should_throwRuntimeException_when_geminiReturnsBlankAnswer() {
        // Arrange
        String question = "What is the loss function?";
        List<RetrievalResult> chunks = List.of(makeRetrievalResult(0, "Some context.", 0.8));

        when(retrievalService.retrieveTopChunks(question)).thenReturn(chunks);

        GenerateContentResponse mockResponse = mock(GenerateContentResponse.class);
        when(mockResponse.text()).thenReturn("  ");
        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class))).thenReturn(mockResponse);

        // Act & Assert
        assertThatThrownBy(() -> service.answerQuestion(question))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty response");
    }
}
