package com.anthonyrodriguez.papersage_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC configuration that registers global CORS mappings for the PaperSage API.
 *
 * <p>Allows the React/Vite frontend (running on localhost during development) to make
 * cross-origin requests to all {@code /api/**} endpoints. Allowed origins are
 * externalized to {@code application.yaml} under {@code app.cors.allowed-origins} so
 * production domains can be added without touching Java source.</p>
 *
 * <p>Using {@link WebMvcConfigurer} rather than {@code @CrossOrigin} on individual
 * controllers keeps the policy centralized and automatically covers any future
 * endpoints added to the API.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Comma-separated list of allowed origins, e.g.
     * {@code "http://localhost:3000,http://localhost:5173"}.
     * Injected from {@code app.cors.allowed-origins} in {@code application.yaml}.
     */
    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.cors.allowed-origins}") String allowedOriginsProperty) {
        this.allowedOrigins = allowedOriginsProperty.split(",");
    }

    /**
     * Registers a CORS mapping covering all {@code /api/**} paths.
     *
     * <ul>
     *   <li>{@code allowedOrigins} — dev servers for CRA (3000) and Vite (5173)</li>
     *   <li>{@code allowedMethods} — standard REST verbs plus OPTIONS (preflight)</li>
     *   <li>{@code allowedHeaders("*")} — permits Content-Type, Accept, etc. required for multipart uploads</li>
     *   <li>{@code allowCredentials(false)} — no auth cookies in current MVP scope</li>
     *   <li>{@code maxAge(3600)} — browsers cache the preflight response for 1 hour</li>
     * </ul>
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
