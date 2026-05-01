package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for the ask endpoint ({@code POST /api/v1/papers/ask}).
 *
 * @param question natural-language question about the currently indexed paper
 */
public record AskQuestionRequest(
        @JsonProperty("question") String question
) {
}
