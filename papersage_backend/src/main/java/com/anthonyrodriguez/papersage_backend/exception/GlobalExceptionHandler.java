package com.anthonyrodriguez.papersage_backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.anthonyrodriguez.papersage_backend.dto.ErrorResponse;
import com.google.genai.errors.ApiException;

import java.io.IOException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotACsResearchPaperException.class)
    public ResponseEntity<ErrorResponse> handleNotACsResearchPaper(NotACsResearchPaperException ex) {
        logger.warn("Guardrail rejected upload: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("NOT_A_CS_RESEARCH_PAPER",
                        "This document does not appear to be a CS research paper. PaperSage only supports computer science research papers."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        logger.warn("File upload exceeded maximum size: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("PAYLOAD_TOO_LARGE",
                        "File size exceeds the maximum allowed limit. Please upload a smaller file."));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingFilePart(MissingServletRequestPartException ex) {
        logger.warn("Missing required file part: {}", ex.getMessage());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("MISSING_FILE",
                        "Missing required file. Please include a PDF file in the 'file' parameter."));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex) {
        logger.error("Failed to process PDF file: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("PDF_PROCESSING_FAILED",
                        "Failed to extract text from the PDF file. The file may be corrupted or unreadable."));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleGeminiApiException(ApiException ex) {
        logger.error("Gemini API error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("AI_SERVICE_UNAVAILABLE",
                        "The AI service is currently unavailable. Please try again later."));
    }

    @ExceptionHandler(EmbeddingGenerationException.class)
    public ResponseEntity<ErrorResponse> handleEmbeddingGenerationException(EmbeddingGenerationException ex) {
        logger.error("Embedding generation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("EMBEDDING_SERVICE_UNAVAILABLE",
                        "Failed to generate embeddings. The AI embedding service is currently unavailable. Please try again later."));
    }

    @ExceptionHandler(PaperAnalysisGenerationException.class)
    public ResponseEntity<ErrorResponse> handlePaperAnalysisGenerationException(PaperAnalysisGenerationException ex) {
        logger.error("Paper analysis generation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("ANALYSIS_SERVICE_UNAVAILABLE",
                        "Failed to generate analysis. The AI analysis service is currently unavailable. Please try again later."));
    }

    @ExceptionHandler(GuardrailClassificationException.class)
    public ResponseEntity<ErrorResponse> handleGuardrailClassificationException(GuardrailClassificationException ex) {
        logger.error("Guardrail classification failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("GUARDRAIL_SERVICE_UNAVAILABLE",
                        "Failed to classify the uploaded document. The AI guardrail service is currently unavailable. Please try again later."));
    }

    @ExceptionHandler(GroundedAnswerGenerationException.class)
    public ResponseEntity<ErrorResponse> handleGroundedAnswerGenerationException(GroundedAnswerGenerationException ex) {
        logger.error("Grounded answer generation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("GROUNDING_SERVICE_UNAVAILABLE",
                        "Failed to generate grounded answer. The AI Q&A service is currently unavailable. Please try again later."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        logger.error("Unexpected error: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR",
                        "An unexpected error occurred. Please try again later."));
    }
}
