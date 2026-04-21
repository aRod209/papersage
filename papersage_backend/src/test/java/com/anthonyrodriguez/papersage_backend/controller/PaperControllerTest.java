package com.anthonyrodriguez.papersage_backend.controller;

import com.anthonyrodriguez.papersage_backend.dto.AnswerResponse;
import com.anthonyrodriguez.papersage_backend.dto.GlossaryEntry;
import com.anthonyrodriguez.papersage_backend.dto.PaperAnalysisResponse;
import com.anthonyrodriguez.papersage_backend.dto.QueryResponse;
import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import com.anthonyrodriguez.papersage_backend.service.GeminiSummaryService;
import com.anthonyrodriguez.papersage_backend.service.GroundedAnswerService;
import com.anthonyrodriguez.papersage_backend.service.PdfExtractionService;
import com.anthonyrodriguez.papersage_backend.service.PaperGuardrailService;
import com.anthonyrodriguez.papersage_backend.service.SemanticRetrievalService;
import com.anthonyrodriguez.papersage_backend.service.TextChunkingService;
import com.anthonyrodriguez.papersage_backend.service.UploadProgressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaperControllerTest {

    @Mock
    private PdfExtractionService pdfExtractionService;

    @Mock
    private GeminiSummaryService geminiSummaryService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private SemanticRetrievalService semanticRetrievalService;

    @Mock
    private GroundedAnswerService groundedAnswerService;

    @Mock
    private UploadProgressService uploadProgressService;

    @Mock
    private PaperGuardrailService paperGuardrailService;

    @InjectMocks
    private PaperController controller;

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private MockMultipartFile validPdf(String content) {
        return new MockMultipartFile("file", "paper.pdf", "application/pdf", content.getBytes());
    }

    private MockMultipartFile emptyFile() {
        return new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    }

    private MockMultipartFile nonPdfFile() {
        return new MockMultipartFile("file", "doc.txt", "text/plain", "hello".getBytes());
    }

    private PaperAnalysisResponse sampleAnalysis() {
        return new PaperAnalysisResponse(
                List.of("Summary point."),
                List.of("Contribution point."),
                List.of(new GlossaryEntry("Term", "Definition")),
                null
        );
    }

    private RetrievalResult sampleRetrievalResult() {
        TextChunk chunk = new TextChunk("chunk-1", "Some chunk text.", 0, "ABSTRACT");
        return new RetrievalResult(chunk, 0.9);
    }

    // ─── POST /api/v1/papers — upload validation ───────────────────────────────

    @Test
    void should_return400_when_uploadedFileIsEmpty() throws IOException {
        // Act
        ResponseEntity<PaperAnalysisResponse> response = controller.uploadPaper(emptyFile());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return400_when_uploadedFileIsNotPdf() throws IOException {
        // Act
        ResponseEntity<PaperAnalysisResponse> response = controller.uploadPaper(nonPdfFile());

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── POST /api/v1/papers — happy path ────────────────────────────────────

    @Test
    void should_return200WithAnalysis_when_validPdfIsUploaded() throws IOException {
        // Arrange
        when(pdfExtractionService.extractText(any())).thenReturn("Extracted paper text.");
        when(textChunkingService.chunkText(anyString())).thenReturn(List.of());
        when(semanticRetrievalService.getIndexedChunkCount()).thenReturn(0);
        when(geminiSummaryService.analyzePaper(anyString())).thenReturn(sampleAnalysis());

        // Act
        ResponseEntity<PaperAnalysisResponse> response = controller.uploadPaper(validPdf("PDF content"));

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().executiveSummary()).containsExactly("Summary point.");
    }

    @Test
    void should_callAllPipelineServices_when_validPdfIsUploaded() throws IOException {
        // Arrange
        List<TextChunk> chunks = List.of(
                new TextChunk("id-1", "Text.", 0, null)
        );
        when(pdfExtractionService.extractText(any())).thenReturn("Some text.");
        when(textChunkingService.chunkText(anyString())).thenReturn(chunks);
        when(semanticRetrievalService.getIndexedChunkCount()).thenReturn(1);
        when(geminiSummaryService.analyzePaper(anyString())).thenReturn(sampleAnalysis());

        // Act
        controller.uploadPaper(validPdf("PDF content"));

        // Assert — verify the full pipeline was executed
        verify(pdfExtractionService).extractText(any());
        verify(textChunkingService).chunkText("Some text.");
        verify(semanticRetrievalService).indexChunks(eq(chunks), any());
        verify(geminiSummaryService).analyzePaper("Some text.");
    }

    // ─── POST /api/v1/papers/query — validation ───────────────────────────────

    @Test
    void should_return400_when_queryQuestionIsNull() {
        // Act
        ResponseEntity<QueryResponse> response = controller.queryPaper(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return400_when_queryQuestionIsBlank() {
        // Act
        ResponseEntity<QueryResponse> response = controller.queryPaper("   ");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── POST /api/v1/papers/query — happy path ───────────────────────────────

    @Test
    void should_return200WithEmptyChunks_when_noPaperIsUploaded() {
        // Arrange
        when(semanticRetrievalService.retrieveTopChunks(anyString())).thenReturn(List.of());

        // Act
        ResponseEntity<QueryResponse> response = controller.queryPaper("What is the contribution?");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().topChunks()).isEmpty();
    }

    @Test
    void should_return200WithTopChunks_when_validQuestionIsAsked() {
        // Arrange
        List<RetrievalResult> results = List.of(sampleRetrievalResult());
        when(semanticRetrievalService.retrieveTopChunks(anyString())).thenReturn(results);

        // Act
        ResponseEntity<QueryResponse> response = controller.queryPaper("What dataset was used?");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().topChunks()).hasSize(1);
        assertThat(response.getBody().question()).isEqualTo("What dataset was used?");
    }

    // ─── POST /api/v1/papers/ask — validation ────────────────────────────────

    @Test
    void should_return400_when_askQuestionIsNull() {
        // Act
        ResponseEntity<AnswerResponse> response = controller.askPaper(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_return400_when_askQuestionIsBlank() {
        // Act
        ResponseEntity<AnswerResponse> response = controller.askPaper("  ");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ─── POST /api/v1/papers/ask — happy path ────────────────────────────────

    @Test
    void should_return200WithAnswer_when_validQuestionIsAsked() {
        // Arrange
        AnswerResponse answer = new AnswerResponse(
                "What are the limitations?",
                "The main limitation is computational cost.",
                List.of()
        );
        when(groundedAnswerService.answerQuestion(anyString())).thenReturn(answer);

        // Act
        ResponseEntity<AnswerResponse> response = controller.askPaper("What are the limitations?");

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().answer()).isEqualTo("The main limitation is computational cost.");
    }

    @Test
    void should_delegateToGroundedAnswerService_when_askQuestionIsValid() {
        // Arrange
        String question = "What is the proposed architecture?";
        AnswerResponse answer = new AnswerResponse(question, "A transformer.", List.of());
        when(groundedAnswerService.answerQuestion(question)).thenReturn(answer);

        // Act
        controller.askPaper(question);

        // Assert
        verify(groundedAnswerService).answerQuestion(question);
    }
}
