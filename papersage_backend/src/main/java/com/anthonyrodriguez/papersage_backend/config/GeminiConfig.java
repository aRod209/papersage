package com.anthonyrodriguez.papersage_backend.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that declares a single, shared {@link Client} bean for all
 * Gemini API interactions.
 *
 * <p>The {@code Client} is stateless and thread-safe, so it is safe to share
 * a single instance across {@code GeminiEmbeddingService}, {@code GeminiSummaryService},
 * and {@code GroundedAnswerService}. Declaring it here as a singleton bean avoids
 * redundant HTTP-client construction and makes every dependent service unit-testable
 * by allowing tests to inject a mock {@code Client} via the constructor.</p>
 */
@Configuration
public class GeminiConfig {

    /**
     * Creates and configures the Gemini {@link Client} using the API key
     * loaded from {@code secrets.properties} (or any other property source).
     *
     * @param apiKey the Gemini API key, injected from {@code ${gemini.api.key}}
     * @return a fully configured {@link Client} instance
     */
    @Bean
    public Client geminiClient(@Value("${gemini.api.key}") String apiKey) {
        return Client.builder().apiKey(apiKey).build();
    }
}
