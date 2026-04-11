package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Standard error response returned by all exception handlers.
 * Provides a consistent error shape for the React frontend.
 */
public record ErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("message") String message
) {
}
