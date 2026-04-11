package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits extracted paper text into overlapping chunks suitable for downstream
 * processing (e.g., embedding, retrieval-augmented generation).
 *
 * <p>Each chunk targets <b>500–900 tokens</b> (approximated at ~4 characters per token)
 * with a small overlap between consecutive chunks so that context is not lost at
 * boundaries. Splitting respects sentence boundaries whenever possible.</p>
 */
@Service
public class TextChunkingService {

    private static final Logger logger = LoggerFactory.getLogger(TextChunkingService.class);

    /** Approximate characters-per-token ratio used for token estimation. */
    private static final int CHARS_PER_TOKEN = 4;

    /** Target chunk size in tokens. */
    private static final int TARGET_MIN_TOKENS = 500;
    private static final int TARGET_MAX_TOKENS = 900;

    /** Overlap between consecutive chunks in tokens. */
    private static final int OVERLAP_TOKENS = 100;

    /** Derived character limits. */
    private static final int MIN_CHUNK_CHARS = TARGET_MIN_TOKENS * CHARS_PER_TOKEN;   // 2000
    private static final int MAX_CHUNK_CHARS = TARGET_MAX_TOKENS * CHARS_PER_TOKEN;   // 3600
    private static final int OVERLAP_CHARS = OVERLAP_TOKENS * CHARS_PER_TOKEN;        // 400

    /**
     * Pattern for detecting academic section headers.
     * Matches lines like "Abstract", "1. Introduction", "II. METHODS", "REFERENCES", etc.
     */
    private static final Pattern SECTION_HEADER_PATTERN = Pattern.compile(
            "^\\s*(?:(?:\\d+\\.?\\s+)|(?:[IVXLC]+\\.?\\s+))?([A-Z][A-Z ]{2,})\\s*$",
            Pattern.MULTILINE
    );

    /**
     * Splits the full document text into overlapping chunks.
     *
     * @param fullText the complete extracted text of the paper
     * @return an ordered list of {@link TextChunk} instances
     */
    public List<TextChunk> chunkText(String fullText) {
        if (fullText == null || fullText.isBlank()) {
            logger.warn("Received empty or null text for chunking");
            return List.of();
        }

        List<TextChunk> chunks = new ArrayList<>();
        int textLength = fullText.length();
        int position = 0;
        int chunkIndex = 0;

        while (position < textLength) {
            // Determine the end boundary for this chunk
            int end = Math.min(position + MAX_CHUNK_CHARS, textLength);

            // If we haven't reached the end of the document, try to break at a sentence boundary
            if (end < textLength) {
                end = findSentenceBoundary(fullText, position + MIN_CHUNK_CHARS, end);
            }

            String chunkText = fullText.substring(position, end);
            String sectionLabel = detectSectionLabel(chunkText);

            chunks.add(new TextChunk(
                    UUID.randomUUID().toString(),
                    chunkText,
                    chunkIndex,
                    sectionLabel
            ));
            chunkIndex++;

            // Advance position with overlap (but don't go backwards)
            int nextPosition = end - OVERLAP_CHARS;
            if (nextPosition <= position) {
                // Safety: always advance at least to 'end' to avoid infinite loop
                nextPosition = end;
            }
            position = nextPosition;

            // If the remaining text is too small to form a meaningful chunk, absorb it
            if (position < textLength && (textLength - position) < (MIN_CHUNK_CHARS / 2)) {
                break;
            }
        }

        // If there's leftover text after the loop, add a final chunk
        if (position < textLength) {
            String remainingText = fullText.substring(position);
            String sectionLabel = detectSectionLabel(remainingText);

            chunks.add(new TextChunk(
                    UUID.randomUUID().toString(),
                    remainingText,
                    chunkIndex,
                    sectionLabel
            ));
        }

        logChunkingSummary(chunks);
        return chunks;
    }

    /**
     * Searches for the last sentence-ending punctuation (.!?) followed by whitespace
     * within the given range and returns the position just after it.
     * Falls back to {@code maxEnd} if no boundary is found.
     */
    private int findSentenceBoundary(String text, int searchStart, int maxEnd) {
        int bestBreak = -1;

        for (int i = maxEnd - 1; i >= searchStart; i--) {
            char c = text.charAt(i);
            if ((c == '.' || c == '!' || c == '?') && i + 1 < text.length()
                    && Character.isWhitespace(text.charAt(i + 1))) {
                bestBreak = i + 1; // include the punctuation, break before whitespace
                break;
            }
        }

        return bestBreak > 0 ? bestBreak : maxEnd;
    }

    /**
     * Attempts to detect an academic section heading at the beginning of the chunk text.
     * Looks at the first few lines for common patterns like "ABSTRACT", "1. INTRODUCTION", etc.
     *
     * @param chunkText the text of the chunk
     * @return the detected section label, or {@code null} if none found
     */
    private String detectSectionLabel(String chunkText) {
        // Only inspect the first 300 characters to find a header near the top
        String preview = chunkText.substring(0, Math.min(chunkText.length(), 300));
        Matcher matcher = SECTION_HEADER_PATTERN.matcher(preview);

        if (matcher.find()) {
            return matcher.group(1).strip();
        }
        return null;
    }

    /**
     * Logs a summary of the chunking result: total count, first chunk info, last chunk info.
     * Uses {@link List#getFirst()} and {@link List#getLast()} from the Java 21
     * {@code SequencedCollection} interface.
     */
    private void logChunkingSummary(List<TextChunk> chunks) {
        logger.info("Chunking complete: {} chunk(s) generated", chunks.size());

        if (chunks.isEmpty()) {
            return;
        }

        TextChunk first = chunks.getFirst();
        int firstPreviewLen = Math.min(first.chunkText().length(), 200);
        logger.info("First chunk — id={}, index={}, section={}, length={} chars, preview: \"{}\"",
                first.chunkId(),
                first.chunkIndex(),
                first.sectionLabel(),
                first.chunkText().length(),
                first.chunkText().substring(0, firstPreviewLen).replaceAll("\\s+", " "));

        TextChunk last = chunks.getLast();
        int lastPreviewLen = Math.min(last.chunkText().length(), 200);
        logger.info("Last chunk  — id={}, index={}, section={}, length={} chars, preview: \"{}\"",
                last.chunkId(),
                last.chunkIndex(),
                last.sectionLabel(),
                last.chunkText().length(),
                last.chunkText().substring(0, lastPreviewLen).replaceAll("\\s+", " "));
    }
}
