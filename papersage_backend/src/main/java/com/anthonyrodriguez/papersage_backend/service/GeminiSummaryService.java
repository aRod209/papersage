package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.PaperAnalysisResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
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
import java.util.Objects;

@Service
public class GeminiSummaryService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiSummaryService.class);
    private static final String MODEL_ID = "gemini-2.5-flash";
    private static final Float MODEL_TEMPERATURE = 0.3f;

    private final Client client;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;
    private final String systemInstruction;

    public GeminiSummaryService(
            Client client,
            @Value("classpath:prompts/analyze-paper.txt") Resource promptResource,
            @Value("classpath:prompts/system-instruction.txt") Resource systemInstructionResource,
            ObjectMapper objectMapper) throws IOException {
        this.client = client;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
        this.systemInstruction = systemInstructionResource.getContentAsString(StandardCharsets.UTF_8);
        logger.info("Gemini client initialized with model: {}", MODEL_ID);
    }

    /**
     * Analyzes the extracted paper text and returns a structured response containing
     * an executive summary, key contributions, and a glossary of important terms.
     *
     * @param text the full extracted text of the research paper
     * @return a {@link PaperAnalysisResponse} ready for frontend consumption
     */
    public PaperAnalysisResponse analyzePaper(String text) {
        String prompt = buildPrompt(text);

        logger.info("Sending analysis request to Gemini (input length: {} chars)", text.length());

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                .temperature(MODEL_TEMPERATURE)
                .build();

        GenerateContentResponse response = client.models.generateContent(MODEL_ID, prompt, config);
        String rawResponse = Objects.requireNonNull(response.text(),
                "Gemini response text must not be null");

        logger.info("Received analysis response from Gemini ({} chars)", rawResponse.length());

        return parseResponse(rawResponse);
    }

    private String buildPrompt(String paperText) {
        return promptTemplate + paperText;
    }

    /**
     * Parses the raw Gemini response into a {@link PaperAnalysisResponse}.
     * Strips markdown code fences if present before parsing.
     */
    private PaperAnalysisResponse parseResponse(String rawResponse) {
        String cleaned = stripMarkdownFences(rawResponse);

        try {
            return objectMapper.readValue(cleaned, PaperAnalysisResponse.class);
        } catch (Exception e) {
            logger.error("Failed to parse Gemini response as JSON. Raw response:\n{}", rawResponse);
            throw new RuntimeException("Failed to parse AI analysis response. The model returned an unexpected format.", e);
        }
    }

    /**
     * Removes markdown code fences (```json ... ``` or ``` ... ```) that the LLM
     * may wrap around its JSON output.
     */
    private String stripMarkdownFences(String text) {
        String trimmed = text.strip();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }

        // Remove opening fence (with optional language tag)
        int firstNewline = trimmed.indexOf('\n');
        if (firstNewline != -1) {
            trimmed = trimmed.substring(firstNewline + 1);
        }
        // Remove closing fence
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.strip();
    }
}
