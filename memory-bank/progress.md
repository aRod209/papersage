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
- ✅ Embedding reliability hardening in `GeminiEmbeddingService` — bounded concurrency, transient retry with exponential backoff, and completion timeout protection
- ✅ Embedding retry-storm mitigation in `GeminiEmbeddingService` — jittered backoff added and safer defaults set (`max-concurrency: 4`, `max-attempts: 2`)
- ✅ Spring DI startup fix for Docker/test context — resolved `GeminiEmbeddingService` constructor ambiguity by switching to a single constructor with typed properties (`GeminiEmbeddingProperties`) and `@ConfigurationPropertiesScan`

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

### Documentation ✅ (Audited & Verified)
- ✅ Root `README.md` — Project overview, shields.io badges, Mermaid architecture diagram (with `GeminiEmbeddingService`), tech stack table, monorepo structure, quick start guide with `secrets.properties` option, API overview, cross-links to sub-READMEs
- ✅ `papersage_backend/README.md` — Full API reference (4 endpoints with accurate JSON schemas), DTO table (10 records including `SourceReference`), project structure, `application.yaml` config + `secrets.properties` setup, error handling matrix (6 handlers), getting started
- ✅ `papersage_frontend/README.md` — Component breakdown, API layer docs, env vars, npm scripts, build instructions
- ✅ Fixed outdated Javadoc in `GeminiEmbeddingService.java` (`text-embedding-004` → `gemini-embedding-001`)
- ✅ All 3 READMEs audited against source code and corrected: DTO field names, JSON examples, config file names, directory listings, component descriptions
- ✅ Root `README.md` quick-start wording clarified so Compose startup is explicitly an alternative path and manual backend/frontend startup steps are clearly marked as alternatives

### DevOps ✅
- ✅ Added `papersage_backend/Dockerfile` for backend containerization
- ✅ Added `papersage_backend/.dockerignore` for backend Docker build-context hygiene
- ✅ Dockerfile uses Java 21 **multi-stage** build (`eclipse-temurin:21-jdk` builder → `eclipse-temurin:21-jre` runtime)
- ✅ Build stage uses **Maven Wrapper** (`mvnw`) for toolchain consistency across local/CI/Docker
- ✅ Runtime stage runs packaged artifact via `java -jar /app/app.jar` (production-friendly; no Maven at runtime)
- ✅ Added inline `# What:` and `# Why:` comments above each Docker instruction for maintainability
- ✅ Added `papersage_frontend/Dockerfile` for frontend containerization (multi-stage Node build → unprivileged Nginx runtime)
- ✅ Added `papersage_frontend/.dockerignore` for frontend Docker build-context hygiene
- ✅ Added root-level `compose.yaml` (modern Compose spec naming) to orchestrate backend + frontend together
- ✅ Compose backend supports both Gemini key sources: env var (`GEMINI_API_KEY`) and mounted `secrets.properties`
- ✅ Fixed Compose key precedence issue by avoiding forced empty interpolation default for `GEMINI_API_KEY`

### Validation ✅
- ✅ End-to-end flow validated with a real PDF and a live Gemini API key

## What's Left to Build
- ❌ Unit test for `PaperGuardrailService`
- ❌ Persistent storage (database for chunks/embeddings)
- ❌ Multi-paper session support
- ❌ Authentication and authorization
- ❌ True batch embedding API usage (still one request per chunk; now guarded by concurrency/retry/timeout controls)
- ❌ Rate limiting / API throttling
- ❌ Integration tests with a real PDF fixture (for `PdfExtractionService`)
- ❌ Frontend error handling for guardrail rejection (HTTP 422) — currently shows a generic error; could show a specific "not a CS paper" message

## Current Status
**Phase**: Full-stack MVP complete with documentation and local orchestration. Backend starts on `http://localhost:8080` (`mvn spring-boot:run`). Frontend dev server runs on `http://localhost:5173` (`npm run dev`). Frontend production build is clean. Three comprehensive READMEs created (root, backend, frontend). Backend and frontend both have Dockerfiles + `.dockerignore`, and root `compose.yaml` now orchestrates both services together. Embedding reliability hardening has been implemented (bounded concurrency + retry/backoff + timeout), and Docker/test context startup has been stabilized via config-driven embedding DI. **End-to-end testing with a real PDF and live Gemini API key has been completed.**

## Known Issues
- Not yet using true batch embedding API requests; still one request per chunk
- Uploading a new paper silently replaces the previous paper's indexed data
- No validation that the PDF actually contains extractable text (scanned/image PDFs will yield empty text)
- The guardrail does not have a dedicated unit test yet (PaperGuardrailService is untested in isolation)
- Frontend shows a generic error for HTTP 422 guardrail rejections; could be made more specific

## Architecture Assessment Snapshot (Quick Scan)
**Assessment date:** 2026-04-20

**Verdict**
- ✅ Architecture is strong for MVP scope (single-user, in-memory, fast iteration)
- ⚠️ Not yet production-ready for multi-user concurrency without targeted refactors

**Top strengths**
- Clear layered backend design (`Controller -> Service -> DTO`) with constructor injection and centralized exception handling
- Good decomposition of AI pipeline responsibilities (extract, classify, chunk, embed, retrieve, summarize, answer)
- Frontend structure is clean and pragmatic for current scope (pages/components/API module)

**Top architecture risks**
- Global singleton SSE emitter (`UploadProgressService`) introduces cross-user collision risk
- Global mutable in-memory retrieval index (`SemanticRetrievalService`) can race under concurrent uploads/queries
- Upload orchestration concentrated in controller boundary (`PaperController`) instead of application workflow layer
- SSE payloads manually assembled as JSON strings instead of typed event serialization

**Priority refactor order**
1. Session/job-scoped progress streams (replace global emitter model)
2. Extract upload orchestration into application/facade workflow service
3. Concurrency-safe retrieval index strategy (immutable snapshots and/or per-session store)
4. Typed SSE event DTO serialization
5. Frontend API boundary cleanup (single API base source + consider JSON body for ask endpoint)

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
13. **Added**: GitHub READMEs — Root (overview + architecture diagram), Backend (full API reference + error handling), Frontend (component breakdown + setup). Fixed stale Javadoc in `GeminiEmbeddingService.java` (`text-embedding-004` → `gemini-embedding-001`)
14. **Audited**: All 3 READMEs verified against source code — corrected DTO field names, JSON response examples, removed non-existent `PdfExtractionException`, updated `application.properties` → `application.yaml`, added `secrets.properties` config, fixed frontend `public/` listing and `SourceBadge` description
15. **Added**: Backend containerization baseline — `papersage_backend/Dockerfile` with Maven Wrapper-based build stage, JRE runtime stage, and per-instruction explanatory comments
16. **Added**: Docker build-context hygiene — `papersage_backend/.dockerignore` and `papersage_frontend/.dockerignore`
17. **Added**: Frontend containerization baseline — `papersage_frontend/Dockerfile` with Node build stage and unprivileged Nginx runtime stage
18. **Hardened**: Embedding reliability — `GeminiEmbeddingService` now applies bounded concurrency + retry/backoff + completion timeout; unit tests expanded for retry/timeout/fail-fast paths
19. **Stabilized**: Docker/test startup path — replaced ambiguous constructor pattern in `GeminiEmbeddingService` with single-constructor DI using `GeminiEmbeddingProperties`; enabled `@ConfigurationPropertiesScan`; externalized tuning defaults under `app.embedding.*`; updated unit tests accordingly
20. **Tuned**: Embedding burst behavior — added jitter to retry backoff in `GeminiEmbeddingService`, reduced default embedding parallelism/retry attempts (`max-concurrency` 6→4, `max-attempts` 3→2), and expanded unit tests to validate jitter range and low-backoff passthrough behavior
21. **Added**: Root `compose.yaml` (modern Compose spec) for full-stack local container orchestration (`backend` + `frontend`)
22. **Fixed**: Compose Gemini key resolution edge case — removed forced-empty interpolation behavior so mounted `secrets.properties` remains effective when env var is unset
23. **Clarified**: Root `README.md` startup flow — Step 2 (Compose) documented as an alternative to Steps 3–4, with explicit skip guidance when Compose is used
24. **Validated**: End-to-end run completed with a real PDF and live Gemini API key
