package com.anthonyrodriguez.papersage_backend.controller;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.anthonyrodriguez.papersage_backend.dto.AnswerResponse;
import com.anthonyrodriguez.papersage_backend.dto.PaperAnalysisResponse;
import com.anthonyrodriguez.papersage_backend.dto.QueryResponse;
import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import com.anthonyrodriguez.papersage_backend.service.GeminiSummaryService;
import com.anthonyrodriguez.papersage_backend.service.GroundedAnswerService;
import com.anthonyrodriguez.papersage_backend.service.PaperGuardrailService;
import com.anthonyrodriguez.papersage_backend.service.PdfExtractionService;
import com.anthonyrodriguez.papersage_backend.service.SemanticRetrievalService;
import com.anthonyrodriguez.papersage_backend.service.TextChunkingService;
import com.anthonyrodriguez.papersage_backend.service.UploadProgressService;

@RestController
@RequestMapping("/api/v1/papers")
public class PaperController {

    private static final Logger logger = LoggerFactory.getLogger(PaperController.class);
    private static final String APPLICATION_PDF = "application/pdf";

    // Embedding accounts for roughly 55 percentage points (20–75%).
    // We reserve 10 for extraction, 5 for guardrail, 5 for chunking, and 10 for analysis.
    private static final int PCT_EXTRACTED    = 10;
    private static final int PCT_CLASSIFIED   = 15;
    private static final int PCT_CHUNKED      = 20;
    private static final int PCT_EMBED_START  = 20;
    private static final int PCT_EMBED_END    = 75;
    private static final int PCT_ANALYZING    = 80;

    private final PdfExtractionService pdfExtractionService;
    private final GeminiSummaryService geminiSummaryService;
    private final TextChunkingService textChunkingService;
    private final SemanticRetrievalService semanticRetrievalService;
    private final GroundedAnswerService groundedAnswerService;
    private final UploadProgressService uploadProgressService;
    private final PaperGuardrailService paperGuardrailService;

    public PaperController(PdfExtractionService pdfExtractionService,
                           GeminiSummaryService geminiSummaryService,
                           TextChunkingService textChunkingService,
                           SemanticRetrievalService semanticRetrievalService,
                           GroundedAnswerService groundedAnswerService,
                           UploadProgressService uploadProgressService,
                           PaperGuardrailService paperGuardrailService) {
        this.pdfExtractionService = pdfExtractionService;
        this.geminiSummaryService = geminiSummaryService;
        this.textChunkingService = textChunkingService;
        this.semanticRetrievalService = semanticRetrievalService;
        this.groundedAnswerService = groundedAnswerService;
        this.uploadProgressService = uploadProgressService;
        this.paperGuardrailService = paperGuardrailService;
    }

    // -------------------------------------------------------------------------
    // SSE progress endpoint
    // -------------------------------------------------------------------------

    /**
     * Opens a Server-Sent Events stream that the frontend subscribes to before
     * calling {@code POST /api/v1/papers}. The upload pipeline pushes named
     * {@code progress} events as each stage completes.
     *
     * <p>Timeout is set to 120 seconds — long enough for very large papers.</p>
     */
    @GetMapping(value = "/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamProgress() {
        SseEmitter emitter = new SseEmitter(120_000L);
        uploadProgressService.register(emitter);
        logger.info("SSE progress stream opened");
        return emitter;
    }

    // -------------------------------------------------------------------------
    // Upload + analysis pipeline
    // -------------------------------------------------------------------------

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaperAnalysisResponse> uploadPaper(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            logger.warn("Upload attempt with empty file");
            return ResponseEntity.badRequest().build();
        }

        // Pattern matching instanceof — avoids redundant cast and null check in one expression
        if (!(file.getContentType() instanceof String contentType)
                || !contentType.equalsIgnoreCase(APPLICATION_PDF)) {
            logger.warn("Upload attempt with invalid content type: {}", file.getContentType());
            return ResponseEntity.badRequest().build();
        }

        logger.info("=== Pipeline Start: name='{}', size={} bytes ===",
                file.getOriginalFilename(), file.getSize());

        try {
            // Stage 1 — extract text
            uploadProgressService.sendProgress("extracting", "Extracting text from PDF…", PCT_EXTRACTED);
            String extractedText = pdfExtractionService.extractText(file);
            logger.info("Extracted {} characters from PDF", extractedText.length());

            // Stage 1.5 — guardrail: verify this is a CS research paper
            uploadProgressService.sendProgress("classifying", "Verifying document is a CS research paper…", PCT_CLASSIFIED);
            paperGuardrailService.verify(extractedText);

            // Stage 2 — chunk text
            uploadProgressService.sendProgress("chunking", "Splitting paper into chunks…", PCT_CHUNKED);
            List<TextChunk> chunks = textChunkingService.chunkText(extractedText);
            logger.info("Paper '{}' chunked into {} pieces", file.getOriginalFilename(), chunks.size());

            // Stage 3 — embed chunks (per-chunk progress reported inside indexChunks)
            semanticRetrievalService.indexChunks(chunks, (completed, total) -> {
                int pct = PCT_EMBED_START
                        + (int) Math.round((double) completed / total * (PCT_EMBED_END - PCT_EMBED_START));
                String msg = "Embedding chunk %d of %d…".formatted(completed, total);
                uploadProgressService.sendProgress("embedding", msg, pct);
            });
            logger.info("Chunks indexed ({} chunks stored in memory)", semanticRetrievalService.getIndexedChunkCount());

            // Stage 4 — LLM analysis
            uploadProgressService.sendProgress("analyzing",
                    "Generating summary and analysis with AI…", PCT_ANALYZING);
            PaperAnalysisResponse analysis = geminiSummaryService.analyzePaper(extractedText);
            logger.info("Analysis generated for file: {}", file.getOriginalFilename());

            // Signal completion
            uploadProgressService.complete();
            logger.info("=== Pipeline Complete: '{}' ===", file.getOriginalFilename());

            return ResponseEntity.ok(analysis);

        } catch (Exception e) {
            uploadProgressService.error(e.getMessage() != null ? e.getMessage() : "An unexpected error occurred");
            logger.error("Pipeline failed for '{}': {}", file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    // -------------------------------------------------------------------------
    // Retrieval endpoints
    // -------------------------------------------------------------------------

    @PostMapping(value = "/query", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<QueryResponse> queryPaper(@RequestParam("question") String question) {
        if (question == null || question.isBlank()) {
            logger.warn("Query attempt with empty question");
            return ResponseEntity.badRequest().build();
        }

        logger.info("Received question: \"{}\"", question);

        List<RetrievalResult> topChunks = semanticRetrievalService.retrieveTopChunks(question);

        if (topChunks.isEmpty()) {
            logger.warn("No chunks available for retrieval — has a paper been uploaded?");
            return ResponseEntity.ok(new QueryResponse(question, List.of()));
        }

        logger.info("Returning {} chunk(s) for question", topChunks.size());

        return ResponseEntity.ok(new QueryResponse(question, topChunks));
    }

    @PostMapping(value = "/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AnswerResponse> askPaper(@RequestParam("question") String question) {
        if (question == null || question.isBlank()) {
            logger.warn("Ask attempt with empty question");
            return ResponseEntity.badRequest().build();
        }

        logger.info("Received ask question: \"{}\"", question);

        AnswerResponse answer = groundedAnswerService.answerQuestion(question);

        logger.info("Grounded answer generated for question: \"{}\"", question);

        return ResponseEntity.ok(answer);
    }
}
