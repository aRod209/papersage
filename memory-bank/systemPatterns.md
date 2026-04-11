# System Patterns — PaperSage

## Architecture Overview
Standard Spring Boot layered architecture: **Controller → Service → DTO**, with no persistence layer (all data is in-memory).

```
┌────────────────────────────────────────────────────────────┐
│                      PaperController                        │
│  GET  /api/v1/papers/progress  (SSE progress stream)        │
│  POST /api/v1/papers           (upload + analyze)           │
│  POST /api/v1/papers/query     (semantic retrieval)         │
│  POST /api/v1/papers/ask       (grounded Q&A)               │
└─────────┬──────────────────────────────┬───────────────────┘
          │                              │
  ┌───────▼───────┐              ┌───────▼──────────┐
  │ Upload Pipeline│              │ Query/Ask Pipeline│
  └───────┬───────┘              └───────┬──────────┘
          │                              │
┌─────────▼──────────┐         ┌─────────▼────────────────┐
│ PdfExtractionService│         │ SemanticRetrievalService  │
│ (PDFBox)            │         │ (cosine similarity)       │
└─────────┬──────────┘         └─────────┬────────────────┘
          │                              │
┌─────────▼──────────┐         ┌─────────▼────────────────┐
│ PaperGuardrailService│        │ GeminiEmbeddingService    │
│ (CS classification) │         │ (query embedding)         │
└─────────┬──────────┘         └───────────────────────────┘
          │
┌─────────▼──────────┐
│ TextChunkingService │
│ (overlapping chunks)│
└─────────┬──────────┘
          │
┌─────────▼──────────────┐
│ GeminiEmbeddingService  │
│ (document embeddings)   │
└─────────┬──────────────┘
          │
┌─────────▼──────────────┐
│ SemanticRetrievalService│
│ (index chunks)          │
└─────────┬──────────────┘
          │
┌─────────▼──────────────┐
│ GeminiSummaryService    │
│ (structured analysis)   │
└────────────────────────┘
```

## SSE Progress Stream (GET /api/v1/papers/progress)
Frontend connects before uploading; the server registers an `SseEmitter` in `UploadProgressService`.
The upload pipeline fires `sendProgress(stage, message, percent)` at each stage, then `complete()`.
Events are named `progress` and carry `{ stage, message, percent }` JSON.
A `done` event is sent when the pipeline completes; an `error` event if it fails.
Emitter timeout: 120 seconds. Only one emitter active at a time (single-paper session model).

## Upload Pipeline (POST /api/v1/papers)
1. Validate file (non-empty, `application/pdf` content type) — uses Java 21 pattern matching instanceof
2. `UploadProgressService.sendProgress("extracting", …, 10)` → `PdfExtractionService.extractText()` — PDFBox text extraction
3. `UploadProgressService.sendProgress("classifying", …, 15)` → `PaperGuardrailService.verify()` — LLM guardrail: sends first 3000 chars to `gemini-2.5-flash` at temperature 0.0 for YES/NO; rejects non-CS documents with `NotACsResearchPaperException` (→ HTTP 422)
4. `UploadProgressService.sendProgress("chunking", …, 20)` → `TextChunkingService.chunkText()` — Split into overlapping chunks
5. `SemanticRetrievalService.indexChunks(chunks, callback)` → `GeminiEmbeddingService.embedDocuments(texts, callback)` — Embed and store; callback fires `sendProgress("embedding", …, 20–75)` per chunk
6. `UploadProgressService.sendProgress("analyzing", …, 80)` → `GeminiSummaryService.analyzePaper()` — Send full text to Gemini 2.5 Flash, parse JSON response
7. `UploadProgressService.complete()` (sends `done` event, closes SSE stream)
8. Return `PaperAnalysisResponse`

## Query Pipeline (POST /api/v1/papers/query)
1. Validate question (non-blank)
2. `SemanticRetrievalService.retrieveTopChunks()` — Embed question, compute cosine similarity, return top-5
3. Return `QueryResponse` with ranked chunks

## Ask Pipeline — Grounded Q&A (POST /api/v1/papers/ask)
1. Validate question (non-blank)
2. `GroundedAnswerService.answerQuestion()` orchestrates the full flow:
   a. `SemanticRetrievalService.retrieveTopChunks()` — Embed question, retrieve top-5 chunks
   b. Build grounded prompt: insert chunks + question into `grounded-answer.txt` template
   c. Send prompt to Gemini 2.5 Flash (temperature 0.2, Q&A-specific system instruction)
   d. Map retrieved chunks to lightweight `SourceReference` objects
3. Return `AnswerResponse` with answer + source references

## Key Design Decisions

### CS Guardrail
- `PaperGuardrailService.verify()` sends only the first 3000 chars (~750 tokens) to the classifier
- Uses `gemini-2.5-flash` at temperature 0.0 for deterministic YES/NO output
- If response does not start with "YES", throws `NotACsResearchPaperException` → HTTP 422
- Runs before chunking/embedding to avoid wasting Gemini quota on non-CS documents

### In-Memory Storage
- Embedded chunks stored in `ArrayList<EmbeddedChunk>` inside `SemanticRetrievalService`
- Replaced on each new paper upload (single-paper session model)
- No database dependency — simple and fast for MVP

### Chunking Strategy
- Target: 500–900 tokens (~2000–3600 chars at 4 chars/token)
- 100-token overlap (~400 chars) to preserve context at boundaries
- Sentence-boundary-aware splitting (looks for `.!?` followed by whitespace)
- Section header detection via regex for academic patterns (e.g., "1. INTRODUCTION", "ABSTRACT")

### Embedding Strategy
- Model: `gemini-embedding-001` (768 dimensions)
- Task types: `RETRIEVAL_DOCUMENT` for chunks, `RETRIEVAL_QUERY` for questions
- One-at-a-time embedding calls (no batching in current implementation)

### LLM Analysis
- Model: `gemini-2.5-flash` with temperature 0.3
- System instruction: "You are an expert academic CS research analyst."
- Prompt template loaded from `classpath:prompts/analyze-paper.txt`
- Response: raw JSON parsed into `PaperAnalysisResponse` with markdown fence stripping

### Error Handling
- `GlobalExceptionHandler` (`@ControllerAdvice`) provides consistent `ErrorResponse` JSON for:
  - `NotACsResearchPaperException` → 422 (`NOT_A_CS_RESEARCH_PAPER`)
  - `MaxUploadSizeExceededException` → 413 (`PAYLOAD_TOO_LARGE`)
  - `MissingServletRequestPartException` → 400 (`MISSING_FILE`)
  - `IOException` → 422 (`PDF_PROCESSING_FAILED`)
  - `ApiException` (Gemini) → 503 (`AI_SERVICE_UNAVAILABLE`)
  - `RuntimeException` → 500 (`INTERNAL_ERROR`)

### CORS Configuration
- `WebConfig` implements `WebMvcConfigurer` — centralized, covers all `/api/**` endpoints
- Allowed origins externalized to `application.yaml` under `app.cors.allowed-origins`
- Currently: `http://localhost:3000` (CRA) and `http://localhost:5173` (Vite)

## DTO Records
All DTOs use Java `record` types with `@JsonProperty` annotations:
- `PaperAnalysisResponse` — executiveSummary, keyContributions, glossary, prerequisiteKnowledge
- `PrerequisiteKnowledge` — mathTopics (List<String>), aiMlTopics (List<String>)
- `GlossaryEntry` — term, definition
- `TextChunk` — chunkId, chunkText, chunkIndex, sectionLabel
- `EmbeddedChunk` — chunk, embedding (internal only, not serialized)
- `RetrievalResult` — chunk, similarityScore
- `QueryResponse` — question, topChunks
- `AnswerResponse` — question, answer, sources
- `SourceReference` — chunkId, chunkIndex, sectionLabel, similarityScore
- `ErrorResponse` — error, message

## Prompt Templates (classpath:prompts/)
- `analyze-paper.txt` — structured analysis prompt (executiveSummary, keyContributions, glossary, prerequisiteKnowledge)
- `classify-paper.txt` — YES/NO CS classification prompt for guardrail
- `grounded-answer.txt` — RAG grounded Q&A prompt template
- `system-instruction.txt` — Gemini system role ("expert academic CS research analyst")

## Logging Patterns
Comprehensive logging infrastructure is implemented throughout:
- **SLF4J with constructor injection** — All services use `private static final Logger`
- **Detailed retrieval ranking** — `SemanticRetrievalService` logs all chunks with scores, marks top-K with ★
- **Context chunk logging** — `GroundedAnswerService` logs retrieved chunks used for grounded answers
- **Pipeline stage markers** — `=== Pipeline Start/Complete ===` delimiters for clarity
- **Truncation helpers** — `truncate()` methods prevent log pollution while showing previews (e.g., 80–200 char limits)
- **No System.out** — Zero `println` or `printStackTrace()` in codebase per Java rules
