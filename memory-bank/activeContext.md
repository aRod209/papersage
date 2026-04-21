# Active Context — PaperSage

## Current Work Focus
Full-stack MVP is feature-complete. Backend and frontend are both fully built. Project documentation (READMEs) has been created. Root-level Docker Compose orchestration is now added, including dual secret-loading support for Gemini credentials. End-to-end flow has been validated with a real PDF and a live Gemini API key.

## Recent Changes (Most Recent First)

### Architecture Review: Detailed Assessment Saved (MVP vs. Production Readiness)
Completed a full architecture quality review against modern backend/frontend practices and recorded concrete findings.

**Overall verdict:**
- Architecture is **good and pragmatic for MVP scope** (single-user, in-memory, fast iteration).
- Architecture is **not yet production-ready** for multi-user concurrency and long-term extensibility without targeted refactors.

**What is strong (modern practices already present):**
- Clear backend layering (`Controller -> Service -> DTO`) with constructor injection and centralized exception mapping.
- Strong Java 21/Spring patterns (records for DTOs, `@ConfigurationProperties` for embedding tuning, externalized config, Dockerized services).
- Service decomposition is clean across extraction, guardrail, chunking, embedding, retrieval, summarization, and grounded answer generation.
- Frontend is appropriately simple for current product scope (API module + page/component decomposition + local state only).

**Architecture risks / anti-pattern hotspots identified:**
- **Global SSE singleton emitter** in `UploadProgressService` (`AtomicReference<SseEmitter>`) creates cross-user collision risk.
- **Global mutable in-memory retrieval state** in `SemanticRetrievalService` can create race/inconsistency under concurrent upload/query activity.
- **Controller-level orchestration concentration** in `PaperController` keeps too much workflow responsibility at HTTP boundary.
- **Broad catch in controller pipeline** (`catch (Exception e)`) weakens exception-contract clarity at architecture boundaries.
- **Manual JSON string construction for SSE payloads** in `UploadProgressService` is brittle relative to typed DTO serialization.
- **Minor frontend API boundary drift** (duplicate API base resolution; `POST /ask` question passed as query string rather than JSON body).

**Recommended refactor sequence (priority order):**
1. Introduce **session/job-scoped upload progress** (replace global emitter with keyed progress streams).
2. Extract upload orchestration into an **application/facade workflow service** (thin controller boundary).
3. Harden retrieval index concurrency via immutable snapshot swap and/or per-session storage abstraction.
4. Replace manual SSE JSON strings with typed event DTO + serializer.
5. Unify frontend API base config in one place; consider JSON-body contract for ask endpoint.

**Decision framing captured:**
- No immediate rewrite required for MVP.
- Prioritize session isolation + orchestration split before adding persistence/multi-user features.

### Documentation: Root README Quick-Start Clarity Pass
Refined root `README.md` so setup paths are explicit and less ambiguous for new users.

- Added `compose.yaml` to the root project structure tree.
- Replaced placeholder clone URL with the real repository URL: `https://github.com/aRod209/papersage.git`.
- Clarified startup flow semantics:
  - Step 2 is now labeled as an alternative Compose path.
  - Added explicit note: if Step 2 is used, Steps 3 and 4 can be skipped.
  - Relabeled Steps 3 and 4 as manual alternatives.

### DevOps: Root Compose Orchestration Added + Secret Precedence Fix
Added modern Compose-spec orchestration at the repo root and then tuned backend secret wiring after runtime key-resolution feedback.

- **`compose.yaml` (new, root)**
  - Uses modern Compose filename/style (`compose.yaml`, no legacy `version:` key).
  - Defines two services: `backend` and `frontend`.
  - `backend` builds from `./papersage_backend`, publishes `8080:8080`, and restarts unless stopped.
  - `frontend` builds from `./papersage_frontend`, publishes `5173:8080`, passes `VITE_API_BASE_URL` build arg (default `http://localhost:8080`), depends on backend, and restarts unless stopped.
- **Backend secret-loading model in Compose**
  - Added `SPRING_CONFIG_IMPORT=optional:classpath:secrets.properties,optional:file:/app/secrets/secrets.properties`.
  - Added bind mount: `${BACKEND_SECRETS_DIR:-./papersage_backend/src/main/resources}:/app/secrets:ro`.
  - Keeps `GEMINI_API_KEY` environment variable support.
- **Key precedence bug fix**
  - Initial compose value `GEMINI_API_KEY: ${GEMINI_API_KEY:-}` could force an empty value when host env was unset, potentially overriding file-based key resolution.
  - Fixed by changing to `GEMINI_API_KEY:` (pass-through/optional), preserving file-based secret behavior when env var is absent.

**User preference captured:**
- Do not run tests/verification commands on behalf of user for compose changes; provide commands and let user run validation locally.

### Reliability: Embedding Retry Jitter + Lower Default Burst Pressure
Follow-up tuning was applied to reduce retry storms after concerns that virtual-thread parallelism was causing request bursts.

- **`GeminiEmbeddingService`**
  - Added jittered retry backoff (`computeJitteredBackoffMillis`) so retry delays are randomized instead of synchronized.
  - Updated retry logging to include both jittered delay and base backoff for troubleshooting.
  - Kept capped exponential growth (`MAX_BACKOFF_MILLIS = 8000`).
- **`application.yaml` defaults tuned**
  - `app.embedding.max-concurrency`: `6` → `4`
  - `app.embedding.max-attempts`: `3` → `2`
  - Goal: reduce burst pressure and secondary retry amplification while keeping resilience.
- **Test coverage expanded** (`GeminiEmbeddingServiceTest`)
  - Added unit test to validate jittered backoff range for base values > 1.
  - Added unit test to validate passthrough behavior for base backoff 0 and 1.

### Stability: Docker Build/Test Context Startup Fixed (Spring DI constructor ambiguity)
Resolved the backend Docker build failure that occurred during `./mvnw clean package` test execution (`contextLoads`), where Spring failed to instantiate `GeminiEmbeddingService` and reported `No default constructor found`.

- Root cause: constructor ambiguity pattern in `GeminiEmbeddingService` after reliability tuning changes introduced multiple constructors.
- Refactor applied to best-practice Spring Boot DI:
  - Added `GeminiEmbeddingProperties` (`@ConfigurationProperties(prefix = "app.embedding")`).
  - Enabled `@ConfigurationPropertiesScan` in `PapersageBackendApplication`.
  - Simplified `GeminiEmbeddingService` to a single constructor: `Client + GeminiEmbeddingProperties`.
  - Externalized embedding defaults in `application.yaml` under `app.embedding`.
  - Updated `GeminiEmbeddingServiceTest` to construct the service with properties objects.
- User confirmed the fix worked.

### Reliability: Embedding Pipeline Hardened (bounded concurrency + retry/backoff + timeout)
Updated `GeminiEmbeddingService` to prevent indefinite stalls during upload-time embedding.

- Added bounded concurrency gate for document embeddings (`maxConcurrency`, default 6) while still using Java 21 virtual threads.
- Added retry logic with exponential backoff for transient failures (`maxAttempts`, `initialBackoffMillis`) and retryable detection for 429/503/timeout-like failures.
- Added completion timeout protection (`completionTimeoutSeconds`, default 30s) so uploads fail fast instead of hanging forever.
- Preserved ordered embedding output and existing per-chunk progress callback contract.
- Added chunk-level failure messaging to improve troubleshooting when a specific embedding task exhausts retries.

**Test updates:**
- `GeminiEmbeddingServiceTest` expanded with retry-success, non-retryable fail-fast, and timeout behavior tests.
- Latest local execution status is environment-dependent (Maven availability varied across sessions); tests should be re-run locally via Maven Wrapper.

### DevOps: Frontend Dockerization Added (Node build + unprivileged Nginx runtime)
Added containerization assets for the frontend:

- **`papersage_frontend/Dockerfile`** — multi-stage build (`node:22-alpine` builder + `nginxinc/nginx-unprivileged:1.27-alpine` runtime), serves built `dist/` on port `8080`.
- **`papersage_frontend/.dockerignore`** — excludes `node_modules`, `dist`, local env files, logs, IDE/OS artifacts, and VCS metadata from build context.

### DevOps: Backend `.dockerignore` Added
Added **`papersage_backend/.dockerignore`** to reduce context size and avoid leaking local secrets/artifacts into Docker builds.

### DevOps: Backend Dockerfile Added (Maven Wrapper + Java 21 Multi-Stage)
Added `papersage_backend/Dockerfile` for containerized backend builds and runtime.

- Build stage now uses `eclipse-temurin:21-jdk` and **Maven Wrapper** (`mvnw`) instead of a pinned Maven image.
- Docker build copies `.mvn/`, `mvnw`, `pom.xml`, and `src/`, then runs `./mvnw -DskipTests clean package`.
- Runtime stage uses `eclipse-temurin:21-jre`, copies only the built jar, exposes port `8080`, and starts via `java -jar /app/app.jar`.
- File includes inline `# What:` and `# Why:` comments above each Docker instruction for maintainability and onboarding clarity.

**Decision rationale:**
- Prefer Maven Wrapper for reproducible build tooling across local, CI, and Docker contexts.
- Keep multi-stage build to minimize runtime image size and avoid shipping build toolchain/source code.

### Documentation: GitHub READMEs — Created & Verified
Created comprehensive documentation, then audited all 3 READMEs against actual source code and corrected all inaccuracies:

- **Root `README.md`** — Project overview with shields.io badges, Mermaid architecture diagram (includes `GeminiEmbeddingService` node), tech stack table, monorepo structure, quick start guide with `secrets.properties` option, API overview table, and cross-links to sub-READMEs.
- **`papersage_backend/README.md`** — Full API reference (4 endpoints with accurate request/response JSON schemas matching actual DTOs), complete DTO table (10 records including `SourceReference`), project structure tree, `application.yaml` configuration, `secrets.properties` setup, error handling matrix (6 exception handlers), and getting started instructions.
- **`papersage_frontend/README.md`** — Component breakdown (2 pages + 8 UI components), API layer documentation, environment variables, npm scripts, and build instructions.

**Corrections applied during audit:**
- **Backend README:** Fixed DTO field names (`AnswerResponse.sources` is `List<SourceReference>` not `List<RetrievalResult>`; `QueryResponse.topChunks` not `results`; `RetrievalResult` has `chunk: TextChunk` + `similarityScore`; `TextChunk` has 4 fields); added missing `SourceReference` record; removed non-existent `PdfExtractionException` from structure tree and error table; updated `application.properties` → `application.yaml`; added `secrets.properties` config option
- **Frontend README:** Fixed `public/` listing (`favicon.svg` + `icons.svg`, not `papersage.svg`); removed non-existent `tailwind.config.js`; corrected `SourceBadge` description (shows section label, not chunk index)
- **Root README:** Added `secrets.properties` option; fixed `application.properties` reference; added `GeminiEmbeddingService` to Mermaid diagram; corrected embedding model name to `gemini-embedding-001`
- **Bug fix:** Corrected outdated Javadoc in `GeminiEmbeddingService.java` (`text-embedding-004` → `gemini-embedding-001`)

### Feature: CS Guardrail Classification
Added a `PaperGuardrailService` that classifies the uploaded document as a CS research paper before the expensive embedding and analysis pipeline runs.

**Backend:**
- **`PaperGuardrailService.java`** (new) — Sends the first 3000 characters of extracted text to `gemini-2.5-flash` at temperature 0.0 with a `classify-paper.txt` prompt. If response does not start with "YES", throws `NotACsResearchPaperException`.
- **`NotACsResearchPaperException.java`** (new) — Unchecked exception mapped to HTTP 422 by `GlobalExceptionHandler` with error code `NOT_A_CS_RESEARCH_PAPER`.
- **`classify-paper.txt`** (new prompt) — YES/NO classification prompt. Describes what qualifies as a CS research paper (algorithms, ML, systems, networking, etc.) and explicitly excludes resumes, textbooks, biology/economics/medical/legal papers.
- **`PaperController.java`** — Added Stage 1.5 in the upload pipeline: calls `paperGuardrailService.verify(extractedText)` between PDF extraction (10%) and chunking (20%), sending progress at 15% (`"classifying"`).
- **`GlobalExceptionHandler.java`** — Added handler for `NotACsResearchPaperException` → HTTP 422 with message "This document does not appear to be a CS research paper. PaperSage only supports computer science research papers."

**SSE progress stages (updated):**
`extracting (10%) → classifying (15%) → chunking (20%) → embedding (20–75%) → analyzing (80%) → done`

### Feature: Georgia Tech Color Theme
Replaced the previous indigo/slate color scheme with the official Georgia Tech brand palette across all 11 frontend files.

**Color palette applied:**
- **Navy Blue `#003057`** — All primary buttons, header/logo text, section headings, progress bar fill, loading spinner, TL;DR banner background, glossary terms
- **Tech Gold `#B3A369`** — All accent elements: bullet dots, source badge dots, focus rings, percentage labels, nav link, TL;DR label text, badge text, browse link underline
- **Deep navy `#002244`** — Button hover states
- **Warm off-white `#F7F5EE`** — Page background gradient, answer card background, dropzone hover fill
- **Deeper gold `#9A8C58`** — Gold hover states, category sub-headers, answer/sources label text
- **GT brand colors defined as CSS custom properties** in `src/index.css` via Tailwind v4 `@theme` block

### Feature: Loading Progress Indicator (SSE)
Added a real-time progress bar that streams pipeline stage updates from the backend to the frontend while a PDF is being processed.

**Backend:**
- **`UploadProgressService.java`** — Singleton `@Service` holding an `AtomicReference<SseEmitter>`. Exposes `register()`, `sendProgress(stage, message, percent)`, `complete()`, and `error()`. Only one emitter active at a time.
- **`PaperController.java`** — Added `GET /api/v1/papers/progress` endpoint that creates a 120s `SseEmitter`, registers it with `UploadProgressService`, and returns it. The `uploadPaper()` handler calls `sendProgress()` at each stage and `complete()` at the end. Errors call `uploadProgressService.error()` before re-throwing.
- **`GeminiEmbeddingService.java`** — Added overloaded `embedDocuments(List<String>, BiConsumer<Integer,Integer>)` — the callback is invoked after each chunk with `(completedCount, totalCount)`.
- **`SemanticRetrievalService.java`** — Added overloaded `indexChunks(List<TextChunk>, BiConsumer<Integer,Integer>)` that threads the callback through to `embedDocuments()`.

**Frontend:**
- **`ProgressBar.jsx`** (new) — Animated GT navy fill bar with stage emoji label, `percent` display, human-readable `message`, and a `useEffect` elapsed-time counter. Shows a "large papers can take 20–30s" hint after 5 seconds.
- **`UploadPage.jsx`** — Replaced `LoadingSpinner` with `ProgressBar`. Before calling `uploadPaper()`, opens an `EventSource` to `/api/v1/papers/progress`. Each `progress` SSE event updates `{ stage, message, percent }` state. The stream is closed on success or error. A 150ms delay between stream open and POST fire ensures the SSE handshake is registered server-side first.

SSE event shape: `{ "stage": "embedding", "message": "Embedding chunk 3 of 12…", "percent": 47 }`

### Feature: Prerequisite Knowledge Section
Added a "Before You Read" section to the results page.

**Backend:**
- Updated `prompts/analyze-paper.txt` to instruct Gemini to return specific math and AI/ML prerequisite topics
- Added `PrerequisiteKnowledge.java` DTO record (`mathTopics: List<String>`, `aiMlTopics: List<String>`)
- Added `prerequisiteKnowledge` field to `PaperAnalysisResponse.java` (now has 4 fields)

**Frontend:**
- Created `PrerequisiteSection.jsx` component — two-column card (📐 Math / 🤖 AI/ML), bullet list per column, "None identified" fallback
- Updated `ResultsPage.jsx` to destructure `prerequisiteKnowledge` from analysis and render `<PrerequisiteSection>` before the TL;DR banner

### Bug Fix: Test Constructor Mismatch + Frontend tsconfig
- **`PaperControllerTest.java`** — Added `null` as the 4th argument to `PaperAnalysisResponse(...)` in the `sampleAnalysis()` helper after `prerequisiteKnowledge` field was added.
- **`GeminiSummaryServiceTest.java`** — Same fix in `buildValidJson()` helper.
- **`papersage_frontend/tsconfig.json`** — Created a minimal `tsconfig.json` for the plain-JS Vite project. Set `"allowJs": true`, `"checkJs": false`, `"include": ["src/**/*"]`. Resolves VS Code "Cannot find type definition file for 'vite/client'" error.
- Backend `mvn clean test` confirmed: all tests passing, `BUILD SUCCESS`.

### Feature: Full-Stack MVP — React Frontend
Complete React/Vite frontend UI built from scratch.

**Key implementation:**
- No router — `useState` view-switch between `UploadPage` and `ResultsPage` in `App.jsx`
- All API calls in `src/api/paperApi.js` — `uploadPaper(file)` and `askQuestion(question)` using native `fetch`
- `UploadDropzone` — drag-and-drop PDF upload with file validation
- `AskSection` — Q&A form with grounded answer display and source badges
- `ResultsPage` — TL;DR banner + Executive Summary + Key Contributions + Glossary + Ask section
- Tailwind v4 — configured via `@import "tailwindcss"` in `index.css` + `@tailwindcss/vite` plugin
- Production build confirmed clean: `npm run build` (40+ modules, 0 errors)

## Next Steps
- User-run verification of `compose.yaml` flow (`docker compose up --build --force-recreate`) with valid Gemini key
- Expand validation with additional paper types and edge-case PDFs (e.g., scanned/image-heavy documents)
- Consider adding a unit test for `PaperGuardrailService`
- Evaluate persistent storage for multi-paper or multi-session support

## Active Decisions & Considerations
- **Quick-start path semantics**: Docker Compose startup (Step 2) is an alternative full-stack path; manual backend/frontend startup (Steps 3–4) is only needed when not using Compose.
- **Compose naming convention**: prefer modern root `compose.yaml` over legacy `docker-compose.yml`
- **Compose secret strategy**: backend supports both env-based (`GEMINI_API_KEY`) and mounted file-based (`secrets.properties`) key loading
- **Avoid empty-env override**: do not force `GEMINI_API_KEY` to empty string in compose interpolation defaults
- **No router**: Only two views exist; `useState` view-switch is sufficient and simpler
- **First executiveSummary bullet as TL;DR**: Backend has no dedicated `tldr` field — the first bullet is promoted as a prominent navy banner on the results page
- **`/api/v1/papers/ask` uses query param**: `POST /api/v1/papers/ask?question=...` — matches backend `@RequestParam`
- **Tailwind v4**: No `tailwind.config.js` needed; `@import "tailwindcss"` in `index.css` + `@tailwindcss/vite` plugin in `vite.config.js`
- **Single-paper session**: Uploading a new paper replaces previous data in backend memory
- **`PaperAnalysisResponse` has 4 fields**: `executiveSummary`, `keyContributions`, `glossary`, `prerequisiteKnowledge` — unit tests must pass all 4 args (use `null` for `prerequisiteKnowledge` where not needed)
- **SSE progress stream**: Frontend opens `EventSource` 150ms before POST to guarantee server-side registration before pipeline starts
- **Guardrail excerpt length**: 3000 characters (~750 tokens) — enough to cover abstract and intro without wasting tokens
- **Java version**: 21 (NOT 17) — `pom.xml` sets `<java.version>21</java.version>`

## Important Patterns & Preferences
- Plain JSX (`.jsx`), no TypeScript in frontend
- Functional components with hooks only
- Local state (`useState`) for all UI state; no global state libraries
- Native `fetch` for API calls; no axios
- All API calls centralized in `src/api/paperApi.js`
- Error handling in every component (loading + error + empty states)
- Accessible: semantic HTML, ARIA labels, keyboard navigation on dropzone
- All Java services use constructor injection (no `@Autowired` field injection)
- Java 21 pattern matching `instanceof` used in `PaperController` for content-type check
- SLF4J for all logging; zero `System.out.println` in codebase
