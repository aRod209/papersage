# RAG Data Contracts

## API Endpoints

All RAG endpoints are under `/api/v1/papers`.

### Upload and Build RAG Index

```http
POST /api/v1/papers
Content-Type: multipart/form-data
```

Request field:

| Field | Type | Required | Description |
|---|---|---:|---|
| `file` | PDF file | Yes | CS research paper PDF to analyze and index. |

Response:

- `PaperAnalysisResponse`
- The response contains structured analysis, while the side effect is that the RAG index is rebuilt in memory.

### Ask the Paper

```http
POST /api/v1/papers/ask
Content-Type: application/json
```

Request body:

```json
{
  "question": "What is the main contribution of this paper?"
}
```

Response body:

```json
{
  "question": "What is the main contribution of this paper?",
  "answer": "The paper's main contribution is ...",
  "sources": [
    {
      "chunkId": "550e8400-e29b-41d4-a716-446655440000",
      "chunkIndex": 2,
      "sectionLabel": "INTRODUCTION",
      "similarityScore": 0.8123
    }
  ]
}
```

Primary source files:

- `PaperController.askPaper(...)`
- `AskQuestionRequest`
- `AnswerResponse`
- `SourceReference`
- `paperApi.js`

### Retrieve Top Matching Chunks

```http
POST /api/v1/papers/query?question=What%20problem%20does%20the%20paper%20solve%3F
```

Response body:

```json
{
  "question": "What problem does the paper solve?",
  "topChunks": [
    {
      "chunk": {
        "chunkId": "550e8400-e29b-41d4-a716-446655440000",
        "chunkText": "...",
        "chunkIndex": 2,
        "sectionLabel": "INTRODUCTION"
      },
      "similarityScore": 0.8123
    }
  ]
}
```

This endpoint exposes raw retrieval results. The `/ask` endpoint is the full RAG endpoint that additionally generates a grounded answer.

## DTO Reference

### `TextChunk`

Source: `dto/TextChunk.java`

| Field | Type | Description |
|---|---|---|
| `chunkId` | `String` | UUID for the chunk. |
| `chunkText` | `String` | Chunk text content. |
| `chunkIndex` | `int` | Zero-based position in the document. |
| `sectionLabel` | `String` or `null` | Detected academic section label when available. |

### `EmbeddedChunk`

Source: `dto/EmbeddedChunk.java`

| Field | Type | Description |
|---|---|---|
| `chunk` | `TextChunk` | Original chunk. |
| `embedding` | `float[]` | Embedding vector returned by Gemini. |

`EmbeddedChunk` is internal and is not returned by the public API.

### `RetrievalResult`

Source: `dto/RetrievalResult.java`

| Field | Type | Description |
|---|---|---|
| `chunk` | `TextChunk` | Matched chunk. |
| `similarityScore` | `double` | Cosine similarity between query and chunk vectors. |

### `QueryResponse`

Source: `dto/QueryResponse.java`

| Field | Type | Description |
|---|---|---|
| `question` | `String` | Original question. |
| `topChunks` | `List<RetrievalResult>` | Ranked retrieval results. |

### `AnswerResponse`

Source: `dto/AnswerResponse.java`

| Field | Type | Description |
|---|---|---|
| `question` | `String` | Original question. |
| `answer` | `String` | Gemini answer generated from retrieved context. |
| `sources` | `List<SourceReference>` | Lightweight references to chunks used as context. |

### `SourceReference`

Source: `dto/SourceReference.java`

| Field | Type | Description |
|---|---|---|
| `chunkId` | `String` | UUID of source chunk. |
| `chunkIndex` | `int` | Zero-based source position in the paper. |
| `sectionLabel` | `String` or `null` | Source section label if detected. |
| `similarityScore` | `double` | Similarity score used for retrieval ranking. |

## Prompt Contract

Source: `papersage_backend/src/main/resources/prompts/grounded-answer.txt`

The grounded answer prompt has two replacement tokens:

| Token | Replaced with |
|---|---|
| `{chunks}` | Formatted top-k retrieved chunks. |
| `{question}` | User question. |

Core prompt rules:

1. Answer only from provided context chunks.
2. Do not use outside knowledge.
3. If the answer is absent, return exactly: `The answer to this question was not found in the provided context.`
4. Use clear, concise language.
5. Do not repeat the question.

## Frontend Contract

Source: `papersage_frontend/src/components/AskSection.jsx`

The frontend assumes `/ask` returns `answer` for the answer card and `sources` for rendering `SourceBadge` components. The frontend does not currently fetch full chunk text for source expansion, so source display is metadata-oriented rather than quote-oriented.

## Error Contract Summary

RAG-related failures are mapped by `GlobalExceptionHandler`.

| Error code | HTTP status | Typical cause |
|---|---:|---|
| `EMBEDDING_SERVICE_UNAVAILABLE` | 503 | Document or query embedding generation failed. |
| `GROUNDING_SERVICE_UNAVAILABLE` | 503 | Grounded answer generation failed. |
| `AI_SERVICE_UNAVAILABLE` | 503 | Raw Gemini API exception not wrapped by a more specific handler. |
| `INTERNAL_ERROR` | 500 | Unexpected runtime failure. |
