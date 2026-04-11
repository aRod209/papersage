# Tech Context — PaperSage

## Technology Stack

### Backend
| Layer        | Technology                          | Version   |
|-------------|--------------------------------------|-----------|
| Language     | Java                                | 21        |
| Framework    | Spring Boot                         | 3.5.11    |
| Build Tool   | Maven (with Maven Wrapper)          | —         |
| PDF Parsing  | Apache PDFBox                       | 3.0.4     |
| AI/LLM       | Google Gemini (google-genai SDK)    | 1.1.0     |
| Code Gen     | Lombok (included but not used)      | (managed) |
| Testing      | Spring Boot Starter Test (JUnit 5 + Mockito + AssertJ) | (managed) |

### Frontend
| Layer        | Technology                          | Version   |
|-------------|--------------------------------------|-----------|
| Language     | JavaScript (plain JSX, no TypeScript) | —        |
| Framework    | React                               | 19.1.0    |
| Build Tool   | Vite                                | 6.3.1     |
| CSS          | Tailwind CSS v4                     | 4.1.3     |
| Vite Plugin  | @tailwindcss/vite                   | 4.1.3     |
| React Plugin | @vitejs/plugin-react                | 4.4.1     |

## AI Models Used
- **Generative**: `gemini-2.5-flash` — paper analysis (summaries, contributions, glossary, prerequisites) at temperature 0.3; guardrail classification at temperature 0.0
- **Embedding**: `gemini-embedding-001` — 768-dimensional text embeddings for semantic retrieval

## Backend Project Structure
```
papersage_backend/
├── pom.xml
├── mvnw / mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/anthonyrodriguez/papersage_backend/
    │   │   ├── PapersageBackendApplication.java          (Spring Boot entry point)
    │   │   ├── config/
    │   │   │   ├── GeminiConfig.java                     (Gemini Client @Bean)
    │   │   │   └── WebConfig.java                        (CORS config via WebMvcConfigurer)
    │   │   ├── controller/
    │   │   │   └── PaperController.java                  (REST endpoints: progress, upload, query, ask)
    │   │   ├── dto/
    │   │   │   ├── PaperAnalysisResponse.java            (4 fields: executiveSummary, keyContributions, glossary, prerequisiteKnowledge)
    │   │   │   ├── PrerequisiteKnowledge.java            (mathTopics, aiMlTopics)
    │   │   │   ├── GlossaryEntry.java                    (term + definition)
    │   │   │   ├── TextChunk.java                        (chunk data)
    │   │   │   ├── EmbeddedChunk.java                    (chunk + embedding, internal)
    │   │   │   ├── RetrievalResult.java                  (chunk + similarity score)
    │   │   │   ├── QueryResponse.java                    (raw retrieval response)
    │   │   │   ├── AnswerResponse.java                   (grounded Q&A response)
    │   │   │   ├── SourceReference.java                  (lightweight chunk source ref)
    │   │   │   └── ErrorResponse.java                    (error shape)
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java           (centralized error handling)
    │   │   │   └── NotACsResearchPaperException.java     (custom unchecked exception → HTTP 422)
    │   │   └── service/
    │   │       ├── PdfExtractionService.java             (PDFBox text extraction)
    │   │       ├── PaperGuardrailService.java            (CS paper classification; YES/NO via Gemini)
    │   │       ├── TextChunkingService.java              (overlapping text chunking)
    │   │       ├── GeminiEmbeddingService.java           (embedding generation; overloaded embedDocuments supports per-chunk callback)
    │   │       ├── SemanticRetrievalService.java         (in-memory vector search; overloaded indexChunks supports per-chunk callback)
    │   │       ├── GeminiSummaryService.java             (Gemini LLM analysis)
    │   │       ├── GroundedAnswerService.java            (RAG grounded Q&A)
    │   │       └── UploadProgressService.java            (SSE emitter management for upload pipeline progress)
    │   └── resources/
    │       ├── application.yaml                          (app config)
    │       ├── secrets.properties                        (gitignored; contains gemini.api.key)
    │       ├── prompts/
    │       │   ├── analyze-paper.txt                     (analysis prompt template)
    │       │   ├── classify-paper.txt                    (YES/NO CS classification prompt)
    │       │   ├── grounded-answer.txt                   (grounded Q&A prompt template)
    │       │   └── system-instruction.txt                (Gemini system role)
    │       ├── static/
    │       └── templates/
    └── test/
        └── java/com/anthonyrodriguez/papersage_backend/
            ├── PapersageBackendApplicationTests.java
            ├── controller/
            │   └── PaperControllerTest.java
            ├── exception/                                (exception handler tests)
            └── service/
                ├── GeminiEmbeddingServiceTest.java
                ├── GeminiSummaryServiceTest.java
                ├── GroundedAnswerServiceTest.java
                ├── SemanticRetrievalServiceTest.java
                └── TextChunkingServiceTest.java
```

## Frontend Project Structure
```
papersage_frontend/
├── package.json
├── vite.config.js
├── tsconfig.json                  (JS-only; allowJs: true, checkJs: false — for VS Code language service)
├── index.html
├── .env.example                   (VITE_API_BASE_URL=http://localhost:8080)
└── src/
    ├── main.jsx                   (React entry point)
    ├── App.jsx                    (view-switch: upload ↔ results, no router)
    ├── index.css                  (Tailwind v4 @import + GT brand @theme block)
    ├── api/
    │   └── paperApi.js            (uploadPaper(file), askQuestion(question))
    ├── assets/
    │   └── hero.png, vite.svg, typescript.svg
    ├── components/
    │   ├── AskSection.jsx         (Q&A form, answer display, source badges)
    │   ├── GlossaryTable.jsx      (two-column definition list)
    │   ├── LoadingSpinner.jsx     (animated spinner, GT navy)
    │   ├── PrerequisiteSection.jsx (two-column Before You Read card)
    │   ├── ProgressBar.jsx        (SSE-driven animated progress bar with elapsed timer)
    │   ├── SourceBadge.jsx        (source chip with section label + similarity score)
    │   ├── SummarySection.jsx     (reusable bullet-list card)
    │   └── UploadDropzone.jsx     (drag-and-drop + click PDF input)
    └── pages/
        ├── UploadPage.jsx         (upload flow: dropzone, SSE EventSource, ProgressBar, error)
        └── ResultsPage.jsx        (TL;DR, summary, contributions, glossary, ask section)
```

## Configuration

### Backend (`application.yaml`)
```yaml
spring:
  application:
    name: papersage_backend
  config:
    import: optional:classpath:secrets.properties
  servlet:
    multipart:
      enabled: true
      max-file-size: 50MB
      max-request-size: 50MB

app:
  cors:
    allowed-origins: "http://localhost:3000,http://localhost:5173"
```

### Backend (`secrets.properties`) — not committed
```
gemini.api.key=YOUR_GEMINI_API_KEY
```

## Build & Run

### Backend
```bash
# Compile
mvn clean compile

# Run (requires secrets.properties with valid Gemini API key)
mvn spring-boot:run

# Package
mvn clean package
```

### Frontend
```bash
# Install dependencies
npm install

# Dev server (http://localhost:5173)
npm run dev

# Production build
npm run build
```

## Development Setup
- JDK 21+ required
- Maven Wrapper included (`mvnw` / `mvnw.cmd`)
- Create `papersage_backend/src/main/resources/secrets.properties` with `gemini.api.key=...`
- Create `papersage_frontend/.env` from `.env.example` if overriding API base URL

## Technical Constraints
- **Single-paper session**: Only one paper's chunks/embeddings are held in memory at a time. Uploading a new paper replaces the previous one.
- **No persistence**: All data is in-memory; lost on application restart.
- **Sequential embedding**: Chunks are embedded one at a time (no batch API call).
- **Gemini API dependency**: Both analysis and embedding require a valid Gemini API key and network connectivity.
- **CS papers only**: `PaperGuardrailService` rejects non-CS documents with HTTP 422 before the expensive pipeline runs.

## Notes
- **Lombok**: Included in `pom.xml` but currently unused — all DTOs use Java records instead of Lombok annotations.
- **Java 21 features in use**: Pattern matching `instanceof` in `PaperController` content-type check; `String.formatted()` for message interpolation; records for all DTOs.
- **`tsconfig.json`**: Only exists for VS Code language service compatibility in the plain-JS frontend (`allowJs: true`, `checkJs: false`, no `types: ["vite/client"]`).

## Frontend Color Theme — Georgia Tech Brand
All indigo/slate colors replaced with official GT brand colors using Tailwind arbitrary value syntax:
```
Navy Blue:    #003057  — buttons, headings, spinner, progress fill, TL;DR banner bg
Tech Gold:    #B3A369  — accent dots, focus rings, badge accents, nav links, % label, TL;DR label
Deep Navy:    #002244  — button hover states
Deep Gold:    #9A8C58  — gold hover, sub-category labels, answer card label text
Off-white:    #F7F5EE  — page bg gradient, answer card bg, dropzone hover fill
```
GT brand CSS custom properties defined in `src/index.css` via Tailwind v4 `@theme`:
```css
@theme {
  --color-gt-navy: #003057;
  --color-gt-navy-deep: #002244;
  --color-gt-gold: #B3A369;
  --color-gt-gold-deep: #9A8C58;
  --color-gt-gold-light: #F7F5EE;
}
```
