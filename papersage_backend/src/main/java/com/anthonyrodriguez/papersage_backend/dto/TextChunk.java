package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single chunk of text extracted from a research paper.
 *
 * <ul>
 *   <li><b>chunkId</b> — unique identifier (UUID) for this chunk</li>
 *   <li><b>chunkText</b> — the actual text content of the chunk</li>
 *   <li><b>chunkIndex</b> — zero-based position of this chunk in the full document</li>
 *   <li><b>sectionLabel</b> — optional detected section heading (e.g., "Abstract", "Introduction"); may be null</li>
 * </ul>
 */
public record TextChunk(
        @JsonProperty("chunkId") String chunkId,
        @JsonProperty("chunkText") String chunkText,
        @JsonProperty("chunkIndex") int chunkIndex,
        @JsonProperty("sectionLabel") String sectionLabel
) {
}
