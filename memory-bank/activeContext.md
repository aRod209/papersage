# Active Context — PaperSage

## Current Work Focus
Full-stack MVP is feature-complete. Backend and frontend are both fully built. Ready for end-to-end testing with a live Gemini API key.

## Recent Changes (Most Recent First)

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
- Run both backend and frontend and do an end-to-end test with a real PDF and a live Gemini API key
- Consider adding a unit test for `PaperGuardrailService`
- Evaluate persistent storage for multi-paper or multi-session support

## Active Decisions & Considerations
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
