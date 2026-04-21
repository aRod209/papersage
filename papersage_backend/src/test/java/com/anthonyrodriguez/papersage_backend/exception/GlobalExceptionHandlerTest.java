package com.anthonyrodriguez.papersage_backend.exception;

import com.anthonyrodriguez.papersage_backend.dto.ErrorResponse;
import com.google.genai.errors.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    // ─── MaxUploadSizeExceededException → 413 ────────────────────────────────

    @Test
    void should_return413_when_maxUploadSizeExceeded() {
        // Arrange
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024L);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @Test
    void should_returnPayloadTooLargeErrorCode_when_maxUploadSizeExceeded() {
        // Arrange
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(50 * 1024 * 1024L);

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSizeExceeded(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("PAYLOAD_TOO_LARGE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── MissingServletRequestPartException → 400 ────────────────────────────

    @Test
    void should_return400_when_filePartIsMissing() {
        // Arrange
        MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingFilePart(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void should_returnMissingFileErrorCode_when_filePartIsMissing() {
        // Arrange
        MissingServletRequestPartException ex = new MissingServletRequestPartException("file");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleMissingFilePart(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("MISSING_FILE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── IOException → 422 ───────────────────────────────────────────────────

    @Test
    void should_return422_when_ioExceptionIsThrown() {
        // Arrange
        IOException ex = new IOException("PDF is corrupted");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIOException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void should_returnPdfProcessingFailedErrorCode_when_ioExceptionIsThrown() {
        // Arrange
        IOException ex = new IOException("PDF is corrupted");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleIOException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("PDF_PROCESSING_FAILED");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── ApiException → 503 ──────────────────────────────────────────────────

    @Test
    void should_return503_when_geminiApiExceptionIsThrown() {
        // Arrange
        ApiException ex = new ApiException(503, "SERVICE_UNAVAILABLE", "Gemini service unavailable");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGeminiApiException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_returnAiServiceUnavailableErrorCode_when_geminiApiExceptionIsThrown() {
        // Arrange
        ApiException ex = new ApiException(503, "SERVICE_UNAVAILABLE", "Gemini service unavailable");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGeminiApiException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("AI_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── EmbeddingGenerationException → 503 ───────────────────────────────────

    @Test
    void should_return503_when_embeddingGenerationExceptionIsThrown() {
        // Arrange
        EmbeddingGenerationException ex = new EmbeddingGenerationException("embedding failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleEmbeddingGenerationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_returnEmbeddingServiceUnavailableErrorCode_when_embeddingGenerationExceptionIsThrown() {
        // Arrange
        EmbeddingGenerationException ex = new EmbeddingGenerationException("embedding failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleEmbeddingGenerationException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("EMBEDDING_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── PaperAnalysisGenerationException → 503 ───────────────────────────────

    @Test
    void should_return503_when_paperAnalysisGenerationExceptionIsThrown() {
        // Arrange
        PaperAnalysisGenerationException ex = new PaperAnalysisGenerationException("analysis failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handlePaperAnalysisGenerationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_returnAnalysisServiceUnavailableErrorCode_when_paperAnalysisGenerationExceptionIsThrown() {
        // Arrange
        PaperAnalysisGenerationException ex = new PaperAnalysisGenerationException("analysis failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handlePaperAnalysisGenerationException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("ANALYSIS_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── GuardrailClassificationException → 503 ───────────────────────────────

    @Test
    void should_return503_when_guardrailClassificationExceptionIsThrown() {
        // Arrange
        GuardrailClassificationException ex = new GuardrailClassificationException("guardrail failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGuardrailClassificationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_returnGuardrailServiceUnavailableErrorCode_when_guardrailClassificationExceptionIsThrown() {
        // Arrange
        GuardrailClassificationException ex = new GuardrailClassificationException("guardrail failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGuardrailClassificationException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("GUARDRAIL_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── GroundedAnswerGenerationException → 503 ──────────────────────────────

    @Test
    void should_return503_when_groundedAnswerGenerationExceptionIsThrown() {
        // Arrange
        GroundedAnswerGenerationException ex = new GroundedAnswerGenerationException("grounding failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGroundedAnswerGenerationException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void should_returnGroundingServiceUnavailableErrorCode_when_groundedAnswerGenerationExceptionIsThrown() {
        // Arrange
        GroundedAnswerGenerationException ex = new GroundedAnswerGenerationException("grounding failure");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleGroundedAnswerGenerationException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("GROUNDING_SERVICE_UNAVAILABLE");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── RuntimeException → 500 ──────────────────────────────────────────────

    @Test
    void should_return500_when_runtimeExceptionIsThrown() {
        // Arrange
        RuntimeException ex = new RuntimeException("Something exploded");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void should_returnInternalErrorCode_when_runtimeExceptionIsThrown() {
        // Arrange
        RuntimeException ex = new RuntimeException("Something exploded");

        // Act
        ResponseEntity<ErrorResponse> response = handler.handleRuntimeException(ex);

        // Assert
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isNotBlank();
    }

    // ─── All handlers return non-null, non-blank message ─────────────────────

    @Test
    void should_alwaysReturnBothErrorAndMessageFields_for_allExceptionTypes() {
        // Arrange & Act
        ResponseEntity<ErrorResponse> r1 = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(1L));
        ResponseEntity<ErrorResponse> r2 = handler.handleMissingFilePart(new MissingServletRequestPartException("file"));
        ResponseEntity<ErrorResponse> r3 = handler.handleIOException(new IOException("io"));
        ResponseEntity<ErrorResponse> r4 = handler.handleGeminiApiException(new ApiException(503, "SERVICE_UNAVAILABLE", "api error"));
        ResponseEntity<ErrorResponse> r5 = handler.handleRuntimeException(new RuntimeException("rt"));
        ResponseEntity<ErrorResponse> r6 = handler.handleEmbeddingGenerationException(new EmbeddingGenerationException("embedding"));
        ResponseEntity<ErrorResponse> r7 = handler.handlePaperAnalysisGenerationException(new PaperAnalysisGenerationException("analysis"));
        ResponseEntity<ErrorResponse> r8 = handler.handleGuardrailClassificationException(new GuardrailClassificationException("guardrail"));
        ResponseEntity<ErrorResponse> r9 = handler.handleGroundedAnswerGenerationException(new GroundedAnswerGenerationException("grounding"));

        // Assert — every response body must have both fields populated
        for (ResponseEntity<ErrorResponse> r : java.util.List.of(r1, r2, r3, r4, r5, r6, r7, r8, r9)) {
            assertThat(r.getBody()).isNotNull();
            assertThat(r.getBody().error()).isNotBlank();
            assertThat(r.getBody().message()).isNotBlank();
        }
    }
}
