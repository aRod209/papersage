package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Represents the focused background knowledge a reader should have before reading a paper.
 *
 * <ul>
 *   <li><b>mathTopics</b> — specific math concepts the paper directly relies on (e.g., "Bayes' Theorem")</li>
 *   <li><b>aiMlTopics</b> — specific AI/ML concepts the paper builds upon (e.g., "Transformer Architecture")</li>
 * </ul>
 */
public record PrerequisiteKnowledge(
        @JsonProperty("mathTopics") List<String> mathTopics,
        @JsonProperty("aiMlTopics") List<String> aiMlTopics
) {
}
