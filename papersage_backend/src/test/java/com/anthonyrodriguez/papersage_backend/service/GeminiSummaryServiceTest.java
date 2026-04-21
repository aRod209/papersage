package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.GlossaryEntry;
import com.anthonyrodriguez.papersage_backend.dto.PaperAnalysisResponse;
import com.anthonyrodriguez.papersage_backend.exception.PaperAnalysisGenerationException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiSummaryServiceTest {

    @Mock
    private Client mockClient;

    @Mock
    private Models mockModels;

    private GeminiSummaryService service;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PROMPT_TEMPLATE = "Analyze the following paper:\n";
    private static final String SYSTEM_INSTRUCTION = "You are an expert academic CS research analyst.";

    @BeforeEach
    void setUp() throws IOException {
        // Client.models is a final field — use ReflectionTestUtils to inject the mock
        ReflectionTestUtils.setField(mockClient, "models", mockModels);

        Resource promptResource = new ByteArrayResource(PROMPT_TEMPLATE.getBytes());
        Resource systemResource  = new ByteArrayResource(SYSTEM_INSTRUCTION.getBytes());

        service = new GeminiSummaryService(mockClient, promptResource, systemResource, OBJECT_MAPPER);
    }

    // ─── Helper: builds a mock GenerateContentResponse with given text ────────

    private GenerateContentResponse buildResponse(String text) {
        GenerateContentResponse response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn(text);
        return response;
    }

    private String buildValidJson(List<String> summary, List<String> contributions, List<GlossaryEntry> glossary)
            throws Exception {
        return OBJECT_MAPPER.writeValueAsString(
                new PaperAnalysisResponse(summary, contributions, glossary, null));
    }

    // ─── Happy path ───────────────────────────────────────────────────────────

    @Test
    void should_returnPaperAnalysisResponse_when_geminiReturnsValidJson() throws Exception {
        // Arrange
        String json = buildValidJson(
                List.of("The paper proposes a new model."),
                List.of("A 10% improvement over baselines."),
                List.of(new GlossaryEntry("Transformer", "An attention-based neural architecture."))
        );
        GenerateContentResponse response = buildResponse(json);

        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        PaperAnalysisResponse result = service.analyzePaper("Some paper text here.");

        // Assert
        assertThat(result.executiveSummary()).containsExactly("The paper proposes a new model.");
        assertThat(result.keyContributions()).containsExactly("A 10% improvement over baselines.");
        assertThat(result.glossary()).hasSize(1);
        assertThat(result.glossary().get(0).term()).isEqualTo("Transformer");
    }

    // ─── Markdown fence stripping ─────────────────────────────────────────────

    @Test
    void should_stripMarkdownFences_when_geminiWrapsJsonInCodeBlock() throws Exception {
        // Arrange
        String rawJson = buildValidJson(
                List.of("Summary point."),
                List.of("Contribution point."),
                List.of()
        );
        String withFences = "```json\n" + rawJson + "\n```";
        GenerateContentResponse response = buildResponse(withFences);

        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        PaperAnalysisResponse result = service.analyzePaper("Paper text.");

        // Assert
        assertThat(result.executiveSummary()).containsExactly("Summary point.");
        assertThat(result.keyContributions()).containsExactly("Contribution point.");
    }

    @Test
    void should_stripPlainCodeFences_when_geminiUsesTripleBacktickWithoutLanguageTag() throws Exception {
        // Arrange
        String rawJson = buildValidJson(List.of("A."), List.of("B."), List.of());
        String withFences = "```\n" + rawJson + "\n```";
        GenerateContentResponse response = buildResponse(withFences);

        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        PaperAnalysisResponse result = service.analyzePaper("Paper text.");

        // Assert
        assertThat(result.executiveSummary()).containsExactly("A.");
    }

    // ─── Malformed JSON ───────────────────────────────────────────────────────

    @Test
    void should_throwPaperAnalysisGenerationException_when_geminiReturnsInvalidJson() {
        // Arrange
        GenerateContentResponse response = buildResponse("This is not JSON at all.");
        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act & Assert
        assertThatThrownBy(() -> service.analyzePaper("Some paper text."))
                .isInstanceOf(PaperAnalysisGenerationException.class)
                .hasMessageContaining("unexpected format");
    }

    // ─── Empty / null glossary ────────────────────────────────────────────────

    @Test
    void should_returnEmptyGlossary_when_geminiReturnsEmptyGlossaryArray() throws Exception {
        // Arrange
        String json = buildValidJson(List.of("Summary."), List.of("Contribution."), List.of());
        GenerateContentResponse response = buildResponse(json);

        when(mockModels.generateContent(anyString(), anyString(), any(GenerateContentConfig.class)))
                .thenReturn(response);

        // Act
        PaperAnalysisResponse result = service.analyzePaper("Paper text.");

        // Assert
        assertThat(result.glossary()).isEmpty();
    }
}
