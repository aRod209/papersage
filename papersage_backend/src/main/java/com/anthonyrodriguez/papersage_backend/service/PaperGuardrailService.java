package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.exception.GuardrailClassificationException;
import com.anthonyrodriguez.papersage_backend.exception.NotACsResearchPaperException;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Guardrail service that classifies an uploaded document to ensure it is a
 * computer science research paper before the expensive embedding and analysis
 * pipeline runs.
 *
 * <p>Uses {@code gemini-2.5-flash} at temperature 0.0 for a fast, deterministic
 * YES/NO classification. Only the first {@value #EXCERPT_LENGTH} characters of
 * the extracted text are sent — enough to cover the abstract and introduction
 * without wasting tokens.</p>
 *
 * <p>If Gemini returns NO the method throws {@link NotACsResearchPaperException},
 * which the {@code GlobalExceptionHandler} maps to HTTP 422.</p>
 */
@Service
public class PaperGuardrailService {

    private static final Logger logger = LoggerFactory.getLogger(PaperGuardrailService.class);

    private static final String CLASSIFIER_MODEL = "gemini-2.5-flash";
    private static final Float CLASSIFIER_TEMPERATURE = 0.0f;

    /**
     * Number of characters from the start of the extracted text used for
     * classification. 3000 chars ≈ 750 tokens — sufficient to see the abstract
     * and beginning of the introduction without bloating the classifier prompt.
     */
    private static final int EXCERPT_LENGTH = 3000;

    private final Client client;
    private final String promptTemplate;

    public PaperGuardrailService(
            Client client,
            @Value("classpath:prompts/classify-paper.txt") Resource classifyPromptResource) throws IOException {
        this.client = client;
        this.promptTemplate = classifyPromptResource.getContentAsString(StandardCharsets.UTF_8);
        logger.info("PaperGuardrailService initialized with model: {}", CLASSIFIER_MODEL);
    }

    /**
     * Verifies that the provided extracted text belongs to a CS research paper.
     *
     * @param extractedText the full text extracted from the uploaded PDF
     * @throws NotACsResearchPaperException if the document is not a CS research paper
     */
    public void verify(String extractedText) {
        String excerpt = extractedText.length() > EXCERPT_LENGTH
                ? extractedText.substring(0, EXCERPT_LENGTH)
                : extractedText;

        String prompt = promptTemplate + excerpt;

        logger.info("Running guardrail classification (excerpt length: {} chars)", excerpt.length());

        GenerateContentConfig config = GenerateContentConfig.builder()
                .temperature(CLASSIFIER_TEMPERATURE)
                .build();

        String rawResponse;
        try {
            GenerateContentResponse response = client.models.generateContent(CLASSIFIER_MODEL, prompt, config);
            rawResponse = Objects.requireNonNull(response.text(),
                    "Guardrail classifier must not return a null response");
        } catch (ApiException e) {
            throw new GuardrailClassificationException("Failed to classify paper with AI guardrail", e);
        }

        String classification = rawResponse.strip().toUpperCase();
        logger.info("Guardrail classification result: '{}'", classification);

        if (!classification.startsWith("YES")) {
            logger.warn("Guardrail rejected document — classifier returned: '{}'", classification);
            throw new NotACsResearchPaperException(
                    "Document classified as non-CS-research-paper by guardrail (response: " + classification + ")");
        }

        logger.info("Guardrail passed — document confirmed as CS research paper");
    }
}
