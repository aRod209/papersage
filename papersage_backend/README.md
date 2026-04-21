# ⚙️ PaperSage Backend

**Spring Boot REST API for AI-powered research paper analysis**

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-Wrapper-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-API-4285F4?logo=google)](https://ai.google.dev/)

---

## Overview

The PaperSage backend is a Spring Boot 3.5 REST API (Java 21) that handles the full paper-analysis pipeline:

1. **Extract** text from an uploaded PDF using Apache PDFBox
2. **Classify** the document as a CS research paper (guardrail via Gemini)
3. **Chunk** the text into overlapping segments for embedding
4. **Embed** each chunk with Gemini `gemini-embedding-001`
5. **Analyze** the full paper with Gemini 2.5 Flash to produce a structured summary
6. **Answer** follow-up questions using RAG (Retrieval-Augmented Generation)

Real-time progress is streamed to the frontend via **Server-Sent Events (SSE)**.

---

## 🛠️ Tech Stack

| Technology           | Version  | Purpose                                |
| -------------------- | -------- | -------------------------------------- |
| Java (JDK)           | 21       | Language runtime                       |
| Spring Boot          | 3.5.11   | Web framework & dependency injection   |
| Apache PDFBox        | 3.0.4    | PDF text extraction                    |
| Google GenAI SDK     | 1.1.0    | Gemini API client (summary, embeddings, Q&A) |
| Jackson              | (bundled)| JSON serialization of DTOs             |
| SLF4J / Logback      | (bundled)| Logging                                |
| Maven Wrapper        | —        | Build tool (no global install needed)  |

---

## 📁 Project Structure

```
papersage_backend/
├── src/main/java/com/anthonyrodriguez/papersage_backend/
│   ├── PapersageBackendApplication.java      # Spring Boot entry point
│   │
│   ├── config/
│   │   ├── GeminiConfig.java                 # Gemini API client bean
│   │   ├── GeminiEmbeddingProperties.java    # Typed app.embedding config properties
│   │   └── WebConfig.java                    # CORS configuration
│   │
│   ├── controller/
│   │   └── PaperController.java              # REST endpoints
│   │
│   ├── dto/
│   │   ├── PaperAnalysisResponse.java        # Structured analysis result
│   │   ├── AnswerResponse.java               # Grounded Q&A answer
│   │   ├── QueryResponse.java                # Raw chunk retrieval result
│   │   ├── RetrievalResult.java              # Single retrieved chunk + score
│   │   ├── GlossaryEntry.java                # Term + definition pair
│   │   ├── PrerequisiteKnowledge.java        # Math & AI/ML topic lists
│   │   ├── ErrorResponse.java                # Standard error payload
│   │   ├── SourceReference.java              # Source citation in answers
│   │   ├── TextChunk.java                    # Text segment with metadata
│   │   └── EmbeddedChunk.java                # Chunk + float[] embedding
│   │
│   ├── exception/
│   │   ├── EmbeddingGenerationException.java # Embedding generation failures
│   │   ├── GlobalExceptionHandler.java       # @ControllerAdvice error handler
│   │   ├── GroundedAnswerGenerationException.java # Grounded Q&A generation failures
│   │   ├── GuardrailClassificationException.java  # Guardrail service failures
│   │   ├── NotACsResearchPaperException.java # Guardrail rejection
│   │   └── PaperAnalysisGenerationException.java  # Analysis generation failures
│   │
│   └── service/
│       ├── GeminiEmbeddingService.java       # Embed text via Gemini
│       ├── GeminiSummaryService.java         # Structured analysis via Gemini
│       ├── GroundedAnswerService.java        # RAG-powered Q&A
│       ├── PaperGuardrailService.java        # CS paper classification
│       ├── PdfExtractionService.java         # PDFBox text extraction
│       ├── SemanticRetrievalService.java     # Chunk indexing & retrieval
│       ├── TextChunkingService.java          # Overlapping text chunking
│       └── UploadProgressService.java        # SSE progress broadcasting
│
├── src/main/resources/
│   ├── application.yaml                      # App configuration
│   ├── secrets.properties                    # Gemini API key (git-ignored)
│   └── prompts/                              # LLM prompt templates
│
├── src/test/java/...                         # Unit tests
├── pom.xml                                   # Maven build file
├── mvnw / mvnw.cmd                           # Maven Wrapper scripts
└── README.md                                 # ← You are here
```

---

## 📡 API Reference

Base path: `/api/v1/papers`

### Upload Paper

```
POST /api/v1/papers
Content-Type: multipart/form-data
```

| Parameter | Type             | Required | Description              |
| --------- | ---------------- | -------- | ------------------------ |
| `file`    | `multipart/file` | ✅       | PDF file (max 50 MB)     |

**Success Response** — `200 OK`

```json
{
  "executiveSummary": [
    "The paper introduces a novel transformer architecture...",
    "Experiments show a 15% improvement over baseline..."
  ],
  "keyContributions": [
    "A new attention mechanism that reduces complexity...",
    "State-of-the-art results on three benchmarks..."
  ],
  "glossary": [
    { "term": "Transformer", "definition": "A neural network architecture based on self-attention..." },
    { "term": "BLEU Score", "definition": "A metric for evaluating machine translation quality..." }
  ],
  "prerequisiteKnowledge": {
    "mathTopics": ["Linear Algebra", "Probability Theory"],
    "aiMlTopics": ["Attention Mechanisms", "Sequence-to-Sequence Models"]
  }
}
```

**Error Responses:**

| Status | Condition                          |
| ------ | ---------------------------------- |
| `400`  | Empty file or non-PDF content type |
| `413`  | File exceeds 50 MB limit          |
| `422`  | Document is not a CS research paper or PDF text extraction fails |
| `503`  | AI service unavailable (classification, embedding, analysis, or Q&A) |
| `500`  | Unexpected internal server error |

---

### Pipeline Progress (SSE)

```
GET /api/v1/papers/progress
Accept: text/event-stream
```

Opens a Server-Sent Events stream. The frontend should subscribe **before** calling the upload endpoint. Events are named `progress` with JSON data:

```json
{
  "stage": "embedding",
  "message": "Embedding chunk 5 of 42…",
  "percent": 35
}
```

Pipeline stages in order: `extracting` → `classifying` → `chunking` → `embedding` → `analyzing` → `done`

**Timeout:** 120 seconds

---

### Ask a Question (RAG)

```
POST /api/v1/papers/ask?question={question}
```

| Parameter  | Type     | Required | Description                           |
| ---------- | -------- | -------- | ------------------------------------- |
| `question` | `string` | ✅       | Natural-language question about the paper |

**Success Response** — `200 OK`

```json
{
  "question": "What optimization algorithm was used?",
  "answer": "The authors used AdamW with a learning rate of 3e-4...",
  "sources": [
    {
      "chunkId": "chunk-12",
      "chunkIndex": 12,
      "sectionLabel": "Training Details",
      "similarityScore": 0.87
    }
  ]
}
```

> Requires a paper to have been uploaded first (chunks must be indexed in memory).

---

### Retrieve Chunks (Raw)

```
POST /api/v1/papers/query?question={question}
```

| Parameter  | Type     | Required | Description                |
| ---------- | -------- | -------- | -------------------------- |
| `question` | `string` | ✅       | Query for semantic search  |

**Success Response** — `200 OK`

```json
{
  "question": "What datasets were used?",
  "topChunks": [
    {
      "chunk": {
        "chunkId": "chunk-7",
        "chunkText": "We evaluate on CIFAR-10, ImageNet, and...",
        "chunkIndex": 7,
        "sectionLabel": "Experiments"
      },
      "similarityScore": 0.92
    }
  ]
}
```

---

## 📦 Data Models (DTOs)

All DTOs use Java 21 `record` types for immutability.

| Record                    | Fields                                                                 |
| ------------------------- | ---------------------------------------------------------------------- |
| `PaperAnalysisResponse`   | `executiveSummary: List<String>`, `keyContributions: List<String>`, `glossary: List<GlossaryEntry>`, `prerequisiteKnowledge: PrerequisiteKnowledge` |
| `GlossaryEntry`           | `term: String`, `definition: String`                                   |
| `PrerequisiteKnowledge`   | `mathTopics: List<String>`, `aiMlTopics: List<String>`                 |
| `AnswerResponse`          | `question: String`, `answer: String`, `sources: List<SourceReference>` |
| `SourceReference`         | `chunkId: String`, `chunkIndex: int`, `sectionLabel: String`, `similarityScore: double` |
| `QueryResponse`           | `question: String`, `topChunks: List<RetrievalResult>`                 |
| `RetrievalResult`         | `chunk: TextChunk`, `similarityScore: double`                          |
| `TextChunk`               | `chunkId: String`, `chunkText: String`, `chunkIndex: int`, `sectionLabel: String` |
| `EmbeddedChunk`           | `chunk: TextChunk`, `embedding: float[]`                               |
| `ErrorResponse`           | `error: String`, `message: String`                                     |

---

## ⚙️ Configuration

### `application.yaml`

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
  embedding:
    max-concurrency: 4
    max-attempts: 2
    initial-backoff-millis: 500
    completion-timeout-seconds: 30
```

| Property                              | Default              | Description                       |
| ------------------------------------- | -------------------- | --------------------------------- |
| `spring.application.name`             | `papersage_backend`  | Application name                  |
| `spring.config.import`                | `optional:classpath:secrets.properties` | Imports API key file |
| `spring.servlet.multipart.max-file-size` | `50MB`            | Maximum upload file size          |
| `spring.servlet.multipart.max-request-size` | `50MB`         | Maximum request size              |
| `app.cors.allowed-origins`            | `localhost:3000,5173`| Allowed CORS origins              |
| `app.embedding.max-concurrency`       | `4`                 | Max parallel in-flight embedding requests |
| `app.embedding.max-attempts`          | `2`                 | Max attempts per chunk (initial + retries) |
| `app.embedding.initial-backoff-millis`| `500`               | Initial retry backoff duration    |
| `app.embedding.completion-timeout-seconds` | `30`          | Timeout for full embedding completion |

### `secrets.properties`

Create this file at `src/main/resources/secrets.properties` (git-ignored):

```properties
gemini.api.key=your-gemini-api-key-here
```

> Alternatively, set the `GEMINI_API_KEY` environment variable instead of using the file.

### CORS

The `WebConfig` class allows cross-origin requests from:
- `http://localhost:3000`
- `http://localhost:5173`

for all `/api/**` routes with methods `GET`, `POST`, `PUT`, `DELETE`, and `OPTIONS`.

---

## 🔥 Error Handling

The `GlobalExceptionHandler` (`@ControllerAdvice`) maps exceptions to consistent JSON error responses:

| Exception                        | HTTP Status | Error Key                    |
| -------------------------------- | ----------- | ---------------------------- |
| `NotACsResearchPaperException`   | `422`       | `NOT_A_CS_RESEARCH_PAPER`    |
| `MaxUploadSizeExceededException` | `413`       | `PAYLOAD_TOO_LARGE`          |
| `MissingServletRequestPartException` | `400`   | `MISSING_FILE`               |
| `IOException`                    | `422`       | `PDF_PROCESSING_FAILED`      |
| `ApiException` (Gemini)          | `503`       | `AI_SERVICE_UNAVAILABLE`     |
| `EmbeddingGenerationException`   | `503`       | `EMBEDDING_SERVICE_UNAVAILABLE` |
| `PaperAnalysisGenerationException` | `503`     | `ANALYSIS_SERVICE_UNAVAILABLE` |
| `GuardrailClassificationException` | `503`     | `GUARDRAIL_SERVICE_UNAVAILABLE` |
| `GroundedAnswerGenerationException` | `503`    | `GROUNDING_SERVICE_UNAVAILABLE` |
| `RuntimeException`               | `500`       | `INTERNAL_ERROR`             |

All error responses follow the schema:

```json
{
  "error": "NOT_A_CS_RESEARCH_PAPER",
  "message": "This document does not appear to be a CS research paper. PaperSage only supports computer science research papers."
}
```

---

## 🚀 Getting Started

### Prerequisites

- **Java 21** — [Download](https://adoptium.net/)
- **Gemini API Key** — [Get one here](https://aistudio.google.com/apikey)

### Run

```bash
# 1. Navigate to the backend directory
cd papersage_backend

# 2. Set your Gemini API key (choose one):

#   Option A – environment variable
export GEMINI_API_KEY=your-key-here        # Linux/macOS
set GEMINI_API_KEY=your-key-here           # Windows CMD
$env:GEMINI_API_KEY="your-key-here"        # PowerShell

#   Option B – secrets.properties file (recommended)
#   Create src/main/resources/secrets.properties with:
#     gemini.api.key=your-key-here
#   (Already imported by application.yaml via spring.config.import)

# 3. Build and run
./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

The API will be available at **http://localhost:8080**.

### Verify

```bash
curl http://localhost:8080/api/v1/papers/progress
# Should open an SSE stream (Ctrl+C to close)
```

### Build Only

```bash
./mvnw clean compile        # Compile without running
./mvnw clean package        # Build JAR
```

### Run with Docker (Backend Only)

```bash
# From the repository root
docker build -t papersage-backend ./papersage_backend

# Run backend container on port 8080
# Option A: pass API key via environment variable
docker run --rm -p 8080:8080 -e GEMINI_API_KEY=your-key-here papersage-backend
```

> Prefer file-based secrets instead of an env var? Mount a host directory containing `secrets.properties` and set `SPRING_CONFIG_IMPORT` to include it.

### Run with Docker Compose (Full Stack)

For backend + frontend orchestration, use the root-level Compose setup documented in [**../README.md**](../README.md).

---

## 🔗 Related

- [**Root README**](../README.md) — Project overview & quick start
- [**Frontend README**](../papersage_frontend/README.md) — React SPA documentation
