package com.anthonyrodriguez.papersage_backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Structured response returned after analyzing a research paper.
 * Designed for direct consumption by a React frontend.
 *
 * <ul>
 *   <li><b>executiveSummary</b> — 5-8 bullet points summarizing the paper</li>
 *   <li><b>keyContributions</b> — 3-7 bullet points highlighting the paper's main contributions</li>
 *   <li><b>glossary</b> — important terms with simple, plain-language definitions</li>
 *   <li><b>prerequisiteKnowledge</b> — focused math and AI/ML background topics a reader should know</li>
 * </ul>
 */
public record PaperAnalysisResponse(
        @JsonProperty("executiveSummary") List<String> executiveSummary,
        @JsonProperty("keyContributions") List<String> keyContributions,
        @JsonProperty("glossary") List<GlossaryEntry> glossary,
        @JsonProperty("prerequisiteKnowledge") PrerequisiteKnowledge prerequisiteKnowledge
) {
}
