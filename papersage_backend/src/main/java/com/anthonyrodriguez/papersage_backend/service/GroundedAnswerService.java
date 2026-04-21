package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.AnswerResponse;
import com.anthonyrodriguez.papersage_backend.dto.RetrievalResult;
import com.anthonyrodriguez.papersage_backend.dto.SourceReference;
import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import com.anthonyrodriguez.papersage_backend.exception.GroundedAnswerGenerationException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Orchestrates the grounded answer generation flow for Ask-the-Paper.
 *
 * <p>Pipeline:</p>
 * <ol>
 *   <li>Retrieve the top-K most relevant chunks via {@link SemanticRetrievalService}</li>
 *   <li>Build a grounded prompt by injecting the chunks and question into a template</li>
 *   <li>Send the prompt to Gemini and receive a grounded answer</li>
 *   <li>Return the answer along with source references for transparency</li>
 * </ol>
 *
 * <p>Gemini is instructed to answer ONLY from the provided context. If the answer
 * cannot be found, it will explicitly say so.</p>
 */
@Service
public class GroundedAnswerService {

    private static final Logger logger = LoggerFactory.getLogger(GroundedAnswerService.class);
    private static final String MODEL_ID = "gemini-2.5-flash";
    private static final Float MODEL_TEMPERATURE = 0.2f;
    private static final String SYSTEM_INSTRUCTION =
            "You are a precise research paper Q&A assistant. "
            + "You answer questions using only the provided context from a research paper. "
            + "You never fabricate information or use outside knowledge.";

    private final SemanticRetrievalService retrievalService;
    private final Client client;
    private final String promptTemplate;

    public GroundedAnswerService(
            SemanticRetrievalService retrievalService,
            Client client,
            @Value("classpath:prompts/grounded-answer.txt") Resource promptResource) throws IOException {
        this.retrievalService = retrievalService;
        this.client = client;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
        logger.info("GroundedAnswerService initialized with model: {}, temperature: {}", MODEL_ID, MODEL_TEMPERATURE);
    }

    /**
     * Generates a grounded answer for the given question using retrieved paper chunks.
     *
     * @param question the user's natural-language question about the paper
     * @return an {@link AnswerResponse} containing the answer and source references
     */
    public AnswerResponse answerQuestion(String question) {
        logger.info("=== Grounded Answer Pipeline Start ===");
        logger.info("User question: \"{}\"", question);

        // Step 1: Retrieve top relevant chunks
        List<RetrievalResult> retrievedChunks = retrievalService.retrieveTopChunks(question);

        if (retrievedChunks.isEmpty()) {
            logger.warn("No chunks available for grounded answer — has a paper been uploaded?");
            return new AnswerResponse(
                    question,
                    "No paper has been uploaded yet. Please upload a paper before asking questions.",
                    List.of()
            );
        }

        logRetrievedChunks(retrievedChunks);

        // Step 2: Build grounded prompt
        String prompt = buildGroundedPrompt(question, retrievedChunks);
        logger.info("Grounded prompt built ({} chars)", prompt.length());

        // Step 3: Send to Gemini
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(SYSTEM_INSTRUCTION)))
                .temperature(MODEL_TEMPERATURE)
                .build();

        logger.info("Sending grounded prompt to Gemini...");
        String answer;
        try {
            GenerateContentResponse response = client.models.generateContent(MODEL_ID, prompt, config);
            answer = Objects.requireNonNull(response.text(),
                    "Gemini must not return a null response for the grounded answer");
        } catch (ApiException e) {
            throw new GroundedAnswerGenerationException("Failed to generate grounded answer with Gemini", e);
        }

        if (answer.isBlank()) {
            logger.error("Gemini returned a blank response for grounded answer");
            throw new GroundedAnswerGenerationException("Gemini returned an empty response for the grounded answer.");
        }

        String trimmedAnswer = answer.strip();
        logger.info("=== Grounded Answer Received ===");
        logger.info("Answer ({} chars): \"{}\"", trimmedAnswer.length(), truncate(trimmedAnswer, 200));

        // Step 4: Build source references
        List<SourceReference> sources = retrievedChunks.stream()
                .map(this::toSourceReference)
                .toList();

        logger.info("=== Grounded Answer Pipeline Complete ===");

        return new AnswerResponse(question, trimmedAnswer, sources);
    }

    /**
     * Builds the grounded prompt by inserting the retrieved chunks and question
     * into the prompt template. Uses a text block for readable chunk formatting.
     */
    private String buildGroundedPrompt(String question, List<RetrievalResult> retrievedChunks) {
        StringBuilder chunksText = new StringBuilder();
        for (int i = 0; i < retrievedChunks.size(); i++) {
            RetrievalResult result = retrievedChunks.get(i);
            TextChunk chunk = result.chunk();
            String label = chunk.sectionLabel() != null ? chunk.sectionLabel() : "Unknown Section";
            chunksText.append("""
                    [Chunk %d | Index: %d | Section: %s]
                    %s

                    """.formatted(i + 1, chunk.chunkIndex(), label, chunk.chunkText()));
        }

        return promptTemplate
                .replace("{chunks}", chunksText.toString().strip())
                .replace("{question}", question);
    }

    /**
     * Converts a {@link RetrievalResult} into a lightweight {@link SourceReference}.
     */
    private SourceReference toSourceReference(RetrievalResult result) {
        TextChunk chunk = result.chunk();
        return new SourceReference(
                chunk.chunkId(),
                chunk.chunkIndex(),
                chunk.sectionLabel(),
                result.similarityScore()
        );
    }

    /**
     * Logs the retrieved chunks used as context for the grounded answer.
     */
    private void logRetrievedChunks(List<RetrievalResult> retrievedChunks) {
        logger.info("Retrieved {} chunk(s) as context for grounded answer:", retrievedChunks.size());
        for (int i = 0; i < retrievedChunks.size(); i++) {
            RetrievalResult result = retrievedChunks.get(i);
            TextChunk chunk = result.chunk();
            logger.info("  Context chunk {}: chunkIndex={}, section={}, score={}, preview=\"{}\"",
                    i + 1,
                    chunk.chunkIndex(),
                    chunk.sectionLabel(),
                    "%.4f".formatted(result.similarityScore()),
                    truncate(chunk.chunkText(), 80));
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }
}
