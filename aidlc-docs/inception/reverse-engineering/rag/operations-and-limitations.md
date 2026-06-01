# RAG Operations and Limitations

## Runtime Configuration

Source: `papersage_backend/src/main/resources/application.yaml`

```yaml
app:
  embedding:
    max-concurrency: 4
    max-attempts: 2
    initial-backoff-millis: 500
    completion-timeout-seconds: 30
```

These values are bound through `GeminiEmbeddingProperties` and consumed by `GeminiEmbeddingService`.

| Setting | Operational effect |
|---|---|
| `max-concurrency` | Caps in-flight chunk embedding requests even though virtual threads are used. |
| `max-attempts` | Limits attempts per chunk embedding request. |
| `initial-backoff-millis` | Starting point for retry backoff. |
| `completion-timeout-seconds` | Maximum wait for each embedding completion poll before failing the upload. |

## Observability

The implementation logs key RAG events with SLF4J:

- Upload pipeline start and completion.
- Extracted text length.
- Chunk count and first/last chunk previews.
- Embedding completion and vector length observed at runtime.
- Retrieval rankings for all chunks, with top-k marked.
- Retrieved chunks used as grounded answer context.
- Answer length and preview.

This is useful for local development and debugging. For production, these logs should be reviewed for privacy and volume because paper text previews can contain sensitive content.

## Operational Characteristics

| Characteristic | Current behavior |
|---|---|
| Storage | In-memory Java list in `SemanticRetrievalService`. |
| Persistence | None. Data is lost on restart. |
| Paper scope | Single currently uploaded paper. |
| User/session scope | Not isolated by user or session. |
| Retrieval algorithm | Brute-force cosine similarity over all indexed chunks. |
| Embedding calls | One API request per chunk, executed with bounded parallelism. |
| Answer grounding | Prompt instructions plus top-k retrieved chunks. |
| Source reporting | Metadata references, not full quotations. |

## Current Strengths

- Simple architecture with clear service responsibilities.
- No database or vector-store dependency for MVP operation.
- Chunk overlap helps preserve context across boundaries.
- Query/document task hints align embeddings with retrieval use cases.
- Top-k source references improve answer transparency.
- Embedding resilience controls reduce stalled uploads and retry storms.
- SSE progress gives users visibility into long-running embedding work.

## Known Limitations

### Single Global Index

`SemanticRetrievalService` stores indexed chunks in a single mutable list. Uploading a new paper replaces the previous index. In a multi-user environment, this can cause users to query another user's most recent paper.

### No Persistent Vector Store

The in-memory index is lost on backend restart. There is no way to retrieve or ask about previously uploaded papers after process termination.

### Brute-Force Retrieval

Cosine similarity is computed against every chunk in memory. This is acceptable for a single research paper but will not scale to many papers or large corpora.

### One Embedding Request Per Chunk

The implementation uses one Gemini embedding request per chunk. Bounded concurrency helps latency, but quota and rate-limit pressure remain relevant for large papers.

### Prompt-Level Grounding Only

Gemini is instructed to use only retrieved chunks, but the system does not perform post-generation citation verification or answer-span validation.

### Limited Source UX

`AnswerResponse.sources` contains metadata only. The UI shows source badges but does not currently display the exact source text behind each answer.

## Production-Readiness Recommendations

1. **Introduce session/job-scoped indexes**
   - Key uploaded papers by session, user, or upload job.
   - Avoid global cross-user collisions.
2. **Move retrieval storage behind an abstraction**
   - Define an interface such as `RetrievalIndexStore`.
   - Keep in-memory storage for local development.
   - Add durable/vector-store implementations later.
3. **Use immutable index snapshots**
   - Build a new index off to the side.
   - Atomically swap it into service once complete.
   - Reduce inconsistent query behavior during indexing.
4. **Add persistent paper metadata**
   - Store paper ID, upload timestamp, chunk metadata, and analysis output.
   - Enable multi-paper history and repeat queries.
5. **Improve citation support**
   - Return source snippets or offsets.
   - Let users inspect source chunks directly from the UI.
   - Consider answer sentence to source alignment.
6. **Add retrieval evaluation tests**
   - Use fixed paper fixtures and expected relevant chunks.
   - Track retrieval quality across chunking or embedding changes.
7. **Harden privacy and logging**
   - Avoid logging raw paper snippets in production.
   - Add structured log fields without sensitive content.
8. **Plan for rate limiting and quota management**
   - Add API-level throttling.
   - Consider queueing or backpressure for large uploads.

## Documentation Drift Notes

During source review, the following documentation drift was observed:

- `GeminiEmbeddingService.java` documents `gemini-embedding-001` and states that embeddings are 3072-dimensional.
- `EmbeddedChunk.java` Javadoc still references `text-embedding-004` and 768 dimensions.
- Some existing project documentation and memory context also mention 768 dimensions.

The safest operational source of truth is the runtime vector length logged by `GeminiEmbeddingService` when the first embedding is generated. Future cleanup should align all Javadocs and docs with the current Gemini model behavior verified in the active SDK/API response.

## Suggested Verification Checklist for Future RAG Changes

- [ ] Upload a representative CS paper and confirm indexing completes.
- [ ] Ask a factual question with an answer clearly present in the paper.
- [ ] Ask a question not answered by the retrieved context and confirm the not-found behavior.
- [ ] Confirm source references are returned and ranked by similarity.
- [ ] Confirm SSE progress reaches embedding and done stages.
- [ ] Run backend unit tests covering chunking, embedding, retrieval, and grounded answer services.
- [ ] Re-check documentation if model IDs, task types, DTOs, or endpoints change.
