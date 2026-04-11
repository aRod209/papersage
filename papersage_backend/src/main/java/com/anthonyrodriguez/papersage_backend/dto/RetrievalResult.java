package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single chunk result from semantic retrieval, paired with its cosine
 * similarity score relative to the user's question.
 *
 * @param chunk           the matched text chunk
 * @param similarityScore cosine similarity between the question and this chunk (0.0–1.0)
 */
public record RetrievalResult(
        @JsonProperty("chunk") TextChunk chunk,
        @JsonProperty("similarityScore") double similarityScore
) {
}
