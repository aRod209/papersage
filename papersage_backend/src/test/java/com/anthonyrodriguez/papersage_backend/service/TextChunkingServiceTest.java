package com.anthonyrodriguez.papersage_backend.service;

import com.anthonyrodriguez.papersage_backend.dto.TextChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    private TextChunkingService service;

    @BeforeEach
    void setUp() {
        service = new TextChunkingService();
    }

    // ─── Null / blank input ───────────────────────────────────────────────────

    @Test
    void should_returnEmptyList_when_textIsNull() {
        // Act
        List<TextChunk> result = service.chunkText(null);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void should_returnEmptyList_when_textIsBlank() {
        // Act
        List<TextChunk> result = service.chunkText("   ");

        // Assert
        assertThat(result).isEmpty();
    }

    // ─── Single chunk (text shorter than MIN_CHUNK_CHARS = 2000) ─────────────

    @Test
    void should_returnSingleChunk_when_textIsShorterThanMinChunkSize() {
        // Arrange
        String shortText = "This is a short research paper abstract.";

        // Act
        List<TextChunk> result = service.chunkText(shortText);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).chunkText()).isEqualTo(shortText);
        assertThat(result.get(0).chunkIndex()).isZero();
    }

    // ─── Multiple chunks (text longer than MAX_CHUNK_CHARS = 3600) ───────────

    @Test
    void should_returnMultipleChunks_when_textExceedsMaxChunkSize() {
        // Arrange — build a text well over 3600 characters
        String longText = "A".repeat(500) + ". " + "B".repeat(500) + ". " +
                          "C".repeat(500) + ". " + "D".repeat(500) + ". " +
                          "E".repeat(500) + ". " + "F".repeat(500) + ". " +
                          "G".repeat(500) + ". " + "H".repeat(500) + ".";

        // Act
        List<TextChunk> result = service.chunkText(longText);

        // Assert
        assertThat(result).hasSizeGreaterThan(1);
    }

    // ─── Chunk IDs are unique UUIDs ───────────────────────────────────────────

    @Test
    void should_assignUniqueChunkId_toEachChunk() {
        // Arrange
        String longText = "Word sentence ending here. ".repeat(300);

        // Act
        List<TextChunk> result = service.chunkText(longText);

        // Assert
        long uniqueIds = result.stream().map(TextChunk::chunkId).distinct().count();
        assertThat(uniqueIds).isEqualTo(result.size());
    }

    // ─── Chunk indices are sequential ────────────────────────────────────────

    @Test
    void should_assignSequentialChunkIndices_startingFromZero() {
        // Arrange
        String longText = "Sentence ends here. ".repeat(300);

        // Act
        List<TextChunk> result = service.chunkText(longText);

        // Assert
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).chunkIndex()).isEqualTo(i);
        }
    }

    // ─── No chunk has empty text ──────────────────────────────────────────────

    @Test
    void should_neverProduceEmptyChunkText() {
        // Arrange
        String longText = "Some content with sentences. ".repeat(300);

        // Act
        List<TextChunk> result = service.chunkText(longText);

        // Assert
        assertThat(result).allSatisfy(chunk ->
                assertThat(chunk.chunkText()).isNotBlank()
        );
    }

    // ─── Section label detection ──────────────────────────────────────────────

    @Test
    void should_detectSectionLabel_when_chunkStartsWithAcademicHeader() {
        // Arrange — text starts with a recognisable all-caps header
        String textWithHeader = "ABSTRACT\n\nThis paper presents a novel approach to machine learning "
                + "that achieves state-of-the-art results on benchmark datasets. "
                + "The method relies on a transformer-based architecture.";

        // Act
        List<TextChunk> result = service.chunkText(textWithHeader);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).sectionLabel()).isEqualTo("ABSTRACT");
    }

    @Test
    void should_returnNullSectionLabel_when_noHeaderDetected() {
        // Arrange — plain prose with no academic header
        String plainText = "This is just a plain sentence without any section heading.";

        // Act
        List<TextChunk> result = service.chunkText(plainText);

        // Assert
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).sectionLabel()).isNull();
    }

    // ─── Overlap: consecutive chunks share text ───────────────────────────────

    @Test
    void should_produceOverlappingChunks_when_textSpansMultipleChunks() {
        // Arrange — enough text to force at least 2 chunks; use sentence-friendly content
        String repeatedSentence = "This is a test sentence that ends properly. ";
        String longText = repeatedSentence.repeat(200); // ~8800 chars, forces 3+ chunks

        // Act
        List<TextChunk> result = service.chunkText(longText);

        // Assert — with overlap, the end of chunk N should appear at the start of chunk N+1
        assertThat(result).hasSizeGreaterThanOrEqualTo(2);

        String endOfFirstChunk = result.get(0).chunkText();
        String startOfSecondChunk = result.get(1).chunkText();

        // The last ~400 chars of chunk 0 should overlap with the start of chunk 1
        String overlapCandidate = endOfFirstChunk.substring(
                Math.max(0, endOfFirstChunk.length() - 500));
        assertThat(startOfSecondChunk).contains(overlapCandidate.substring(0, 50));
    }
}
