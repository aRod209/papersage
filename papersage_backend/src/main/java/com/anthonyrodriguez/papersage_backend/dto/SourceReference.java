package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A lightweight reference to a source chunk used in a grounded answer.
 * Contains identifying metadata without the full chunk text, keeping API responses clean.
 *
 * @param chunkId         unique identifier (UUID) of the source chunk
 * @param chunkIndex      zero-based position of this chunk in the original document
 * @param sectionLabel    detected section heading (e.g., "Abstract", "Introduction"); may be null
 * @param similarityScore cosine similarity between the user's question and this chunk (0.0–1.0)
 */
public record SourceReference(
        @JsonProperty("chunkId") String chunkId,
        @JsonProperty("chunkIndex") int chunkIndex,
        @JsonProperty("sectionLabel") String sectionLabel,
        @JsonProperty("similarityScore") double similarityScore
) {
}
