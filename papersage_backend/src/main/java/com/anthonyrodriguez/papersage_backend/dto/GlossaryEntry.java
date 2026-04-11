package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single glossary term and its plain-language definition.
 */
public record GlossaryEntry(
        @JsonProperty("term") String term,
        @JsonProperty("definition") String definition
) {
}
