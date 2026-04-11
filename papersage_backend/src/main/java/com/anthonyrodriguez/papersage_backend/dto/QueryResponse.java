package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response returned by the semantic retrieval endpoint.
 * Contains the original question and the top-K most semantically similar chunks.
 *
 * @param question  the user's original question
 * @param topChunks ranked list of matching chunks with similarity scores
 */
public record QueryResponse(
        @JsonProperty("question") String question,
        @JsonProperty("topChunks") List<RetrievalResult> topChunks
) {
}
