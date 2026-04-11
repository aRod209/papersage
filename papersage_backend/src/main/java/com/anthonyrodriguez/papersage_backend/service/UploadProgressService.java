package com.anthonyrodriguez.papersage_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages a single active SSE emitter for upload pipeline progress reporting.
 *
 * <p>Lifecycle:</p>
 * <ol>
 *   <li>Frontend connects to {@code GET /api/v1/papers/progress} — controller calls {@link #register(SseEmitter)}.</li>
 *   <li>Upload pipeline calls {@link #sendProgress(String, String, int)} at each stage.</li>
 *   <li>Pipeline calls {@link #complete()} when done, or {@link #error(String)} on failure.</li>
 * </ol>
 *
 * <p>Only one emitter is active at a time, matching the single-paper session model.
 * A new call to {@link #register(SseEmitter)} replaces the previous emitter.</p>
 */
@Service
public class UploadProgressService {

    private static final Logger logger = LoggerFactory.getLogger(UploadProgressService.class);

    private final AtomicReference<SseEmitter> currentEmitter = new AtomicReference<>();

    /**
     * Registers a new SSE emitter, replacing any previously active one.
     *
     * @param emitter the emitter created by the controller for the connecting client
     */
    public void register(SseEmitter emitter) {
        SseEmitter previous = currentEmitter.getAndSet(emitter);
        if (previous != null) {
            try {
                previous.complete();
            } catch (Exception ignored) {
                // previous emitter may already be closed
            }
        }
        logger.info("SSE emitter registered for upload progress");
    }

    /**
     * Sends a progress event to the connected client.
     *
     * @param stage   short identifier for the current pipeline stage (e.g. "embedding")
     * @param message human-readable status message
     * @param percent completion percentage 0–100
     */
    public void sendProgress(String stage, String message, int percent) {
        SseEmitter emitter = currentEmitter.get();
        if (emitter == null) {
            logger.debug("No active SSE emitter — skipping progress event: stage={}, pct={}", stage, percent);
            return;
        }

        String json = buildJson(stage, message, percent);
        logger.debug("Sending SSE progress: stage={}, pct={}, message=\"{}\"", stage, percent, message);

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(json));
        } catch (IOException e) {
            logger.warn("Failed to send SSE progress event — client may have disconnected: {}", e.getMessage());
            currentEmitter.compareAndSet(emitter, null);
        }
    }

    /**
     * Sends a final {@code done} event and completes the SSE stream.
     */
    public void complete() {
        SseEmitter emitter = currentEmitter.getAndSet(null);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("progress")
                    .data(buildJson("done", "Analysis complete!", 100)));
            emitter.complete();
            logger.info("SSE emitter completed — pipeline done");
        } catch (IOException e) {
            logger.warn("Failed to send SSE completion event: {}", e.getMessage());
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // already closed
            }
        }
    }

    /**
     * Sends an error event and completes the SSE stream exceptionally.
     *
     * @param message error description to surface to the client
     */
    public void error(String message) {
        SseEmitter emitter = currentEmitter.getAndSet(null);
        if (emitter == null) {
            return;
        }

        String errorJson = """
                {"error":true,"message":"%s"}
                """.formatted(escapeJson(message)).strip();

        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(errorJson));
        } catch (IOException e) {
            logger.warn("Failed to send SSE error event: {}", e.getMessage());
        } finally {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // already closed
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a JSON progress payload using {@link String#formatted(Object...)} for
     * safe, readable string construction.
     */
    private String buildJson(String stage, String message, int percent) {
        return """
                {"stage":"%s","message":"%s","percent":%d}
                """.formatted(escapeJson(stage), escapeJson(message), percent).strip();
    }

    /** Minimal JSON string escaping (backslash and double-quote only). */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
