# Progress — PaperSage

## What Works

### Backend ✅
- ✅ PDF file upload with validation (type check via Java 21 pattern matching instanceof, size limit via Spring config)
- ✅ Text extraction from PDF using Apache PDFBox
- ✅ Text chunking with overlapping windows and sentence-boundary detection
- ✅ Academic section header detection in chunks (regex-based)
- ✅ **CS Guardrail** — `PaperGuardrailService` classifies first 3000 chars via Gemini at temperature 0.0; rejects non-CS documents with HTTP 422 (`NOT_A_CS_RESEARCH_PAPER`) before embedding/analysis runs
- ✅ Document embedding generation via Gemini embedding API (`gemini-embedding-001`)
- ✅ In-memory chunk indexing and storage
- ✅ Semantic retrieval via cosine similarity (top-5 results)
- ✅ Structured paper analysis via Gemini 2.5 Flash (executive summary, key contributions, glossary, prerequisite knowledge)
- ✅ JSON response parsing with markdown fence stripping
- ✅ Centralized exception handling with consistent error response format (`GlobalExceptionHandler`)
- ✅ Custom exception: `NotACsResearchPaperException` → HTTP 422
- ✅ REST API endpoints: `POST /api/v1/papers`, `POST /api/v1/papers/query`, `POST /api/v1/papers/ask`
- ✅ Grounded answer generation via `POST /api/v1/papers/ask` — full RAG loop closed
- ✅ Source references returned with every grounded answer (chunkId, chunkIndex, sectionLabel, similarityScore)
- ✅ CORS configured for `http://localhost:5173` (Vite) and `http://localhost:3000` (CRA) via `WebConfig`
- ✅ Full unit test suite — all tests passing (JUnit 5 + Mockito + AssertJ)
- ✅ SSE progress endpoint `GET /api/v1/papers/progress` — streams `{ stage, message, percent }` events during upload pipeline
- ✅ `UploadProgressService` — singleton emitter manager; fires per-stage and per-chunk progress events
- ✅ Upload pipeline stages: extracting (10%) → classifying (15%) → chunking (20%) → embedding (20–75%) → analyzing (80%) → done

### Frontend ✅
- ✅ Vite + React 19.1.0 project scaffolded in `papersage_frontend/`
- ✅ Tailwind CSS v4 (4.1.3) integrated via `@tailwindcss/vite` plugin
- ✅ API layer (`src/api/paperApi.js`) — `uploadPaper(file)` and `askQuestion(question)` using native `fetch`
- ✅ `LoadingSpinner` component — animated spinner with label (GT navy)
- ✅ `SummarySection` component — reusable bullet-list card
- ✅ `GlossaryTable` component — two-column definition list
- ✅ `SourceBadge` component — source chip with section label and similarity score
- ✅ `UploadDropzone` component — drag-and-drop + click PDF input with validation
- ✅ `AskSection` component — Q&A form with answer display and source badges
- ✅ `PrerequisiteSection` component — two-column "Before You Read" card (📐 Math / 🤖 AI/ML)
- ✅ `ProgressBar` component — SSE-driven animated GT navy fill bar, stage emoji labels, elapsed-time counter, 20–30s hint after 5s
- ✅ `UploadPage` — full upload flow: SSE `EventSource` (150ms head start before POST), `ProgressBar` during upload, error handling
- ✅ `ResultsPage` — Prerequisite section + TL;DR banner + Executive Summary + Key Contributions + Glossary + Ask section
- ✅ `App.jsx` — view-switch state (upload ↔ results), no router needed
- ✅ `.env.example` — `VITE_API_BASE_URL=http://localhost:8080`
- ✅ Production build succeeds (`npm run build`) — 0 errors
- ✅ Georgia Tech brand color theme — Navy `#003057`, Tech Gold `#B3A369`, warm off-white `#F7F5EE` across all 11 frontend files; GT colors defined as CSS custom properties in `index.css` via Tailwind v4 `@theme`

## What's Left to Build
- ❌ End-to-end test with a real PDF (requires backend running with Gemini API key)
- ❌ Unit test for `PaperGuardrailService`
- ❌ Persistent storage (database for chunks/embeddings)
- ❌ Multi-paper session support
- ❌ Authentication and authorization
- ❌ Batch embedding optimization (currently sequential — one call per chunk)
- ❌ Rate limiting / API throttling
- ❌ Integration tests with a real PDF fixture (for `PdfExtractionService`)
- ❌ Frontend error handling for guardrail rejection (HTTP 422) — currently shows a generic error; could show a specific "not a CS paper" message

## Current Status
**Phase**: Full-stack MVP complete and confirmed building. Backend starts on `http://localhost:8080` (`mvn spring-boot:run`). Frontend dev server runs on `http://localhost:5173` (`npm run dev`). All backend unit tests pass. Frontend production build is clean. **Ready for end-to-end testing with a live Gemini API key.**

## Known Issues
- Sequential embedding calls may be slow for large papers with many chunks
- Uploading a new paper silently replaces the previous paper's indexed data
- No validation that the PDF actually contains extractable text (scanned/image PDFs will yield empty text)
- The guardrail does not have a dedicated unit test yet (PaperGuardrailService is untested in isolation)
- Frontend shows a generic error for HTTP 422 guardrail rejections; could be made more specific

## Evolution of Project Decisions
1. **Started with**: Simple PDF upload → text extraction → LLM summary
2. **Added**: Text chunking and embedding for semantic retrieval capability
3. **Added**: Full RAG pipeline — grounded answer generation via `POST /api/v1/papers/ask`
4. **Refactored**: Extracted `GeminiConfig` bean for DI-compliant client wiring
5. **Added**: Full unit test suite (JUnit 5 + Mockito + AssertJ); all tests passing
6. **Added**: CORS configuration via `WebConfig` (centralized, externalized to `application.yaml`)
7. **Added**: React + Vite + Tailwind CSS v4 frontend — complete UI for upload, results, and Q&A
8. **Added**: `PrerequisiteKnowledge` DTO and `PrerequisiteSection` frontend component — "Before You Read" section
9. **Added**: SSE-based loading progress indicator — `UploadProgressService` + `GET /api/v1/papers/progress` on backend; `ProgressBar.jsx` + `EventSource` integration in `UploadPage.jsx` on frontend
10. **Restyled**: Georgia Tech brand color theme — replaced indigo/slate with Navy `#003057` + Tech Gold `#B3A369` + warm off-white `#F7F5EE`; GT color tokens defined in `index.css` `@theme`
11. **Added**: CS Guardrail — `PaperGuardrailService` + `classify-paper.txt` + `NotACsResearchPaperException` — rejects non-CS documents at stage 1.5 of the upload pipeline before embedding/analysis
12. **Corrected**: Java version — `pom.xml` uses Java 21 (not 17 as previously documented)
