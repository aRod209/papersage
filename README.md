# 📄 PaperSage

**AI-powered research paper summarization & Q&A**

[![Java 21](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot 3.5](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot)](https://spring.io/projects/spring-boot)
[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![Vite 6](https://img.shields.io/badge/Vite-6-646CFF?logo=vite)](https://vite.dev/)
[![Tailwind CSS 4](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?logo=tailwindcss)](https://tailwindcss.com/)
[![Google Gemini](https://img.shields.io/badge/Google%20Gemini-API-4285F4?logo=google)](https://ai.google.dev/)

---

## Overview

PaperSage lets you upload a Computer Science research paper (PDF) and instantly receive a structured, actionable analysis — plus the ability to ask natural-language questions about the paper's content.

The service extracts text, verifies the document is a CS paper, generates a structured summary using Google Gemini, and provides a RAG-powered semantic Q&A pipeline — all streamed to the browser with real-time progress updates.

---

## ✨ Features

- **PDF Upload & Text Extraction** — Upload PDFs up to 50 MB; text is extracted via Apache PDFBox.
- **CS Guardrail Classification** — Gemini verifies the document is a CS research paper before processing (rejects non-CS papers with HTTP 422).
- **Structured Analysis** — Generates:
  - Executive summary (5–8 bullet points)
  - Key contributions (3–7 bullet points)
  - Glossary of important terms (5–15 entries with plain-language definitions)
  - Prerequisite knowledge (math + AI/ML topics)
- **Semantic Q&A (RAG)** — Chunks text, embeds with Gemini `gemini-embedding-001`, retrieves top-k relevant chunks, and generates grounded answers with source citations.
- **Real-Time Progress** — Server-Sent Events (SSE) stream pipeline progress to a live progress bar in the browser.

---

## 🏗️ Architecture

```mermaid
graph LR
    subgraph Frontend ["papersage_frontend (React + Vite)"]
        A[UploadPage] -->|PDF file| B[paperApi.js]
        C[ResultsPage] -->|question| B
        B -->|SSE| D[ProgressBar]
    end

    subgraph Backend ["papersage_backend (Spring Boot)"]
        E[PaperController]
        F[PdfExtractionService]
        G[PaperGuardrailService]
        H[TextChunkingService]
        I[SemanticRetrievalService]
        J[GeminiSummaryService]
        K[GroundedAnswerService]
        L[UploadProgressService]
    end

    subgraph External ["Google Gemini API"]
        M[Gemini 2.5 Flash]
        N[gemini-embedding-001]
    end

    B -->|POST /api/v1/papers| E
    B -->|POST /api/v1/papers/ask| E
    B -->|GET /api/v1/papers/progress| L

    E --> F --> G --> H --> I --> J
    E --> K
    I --> O[GeminiEmbeddingService] --> N
    J --> M
    K --> M
    G --> M
```

---

## 🛠️ Tech Stack

| Layer      | Technology                                                                  |
| ---------- | --------------------------------------------------------------------------- |
| Frontend   | React 19, Vite 6, Tailwind CSS 4, JavaScript/JSX                           |
| Backend    | Java 21, Spring Boot 3.5, Apache PDFBox 3.0, Maven                         |
| AI / LLM   | Google Gemini 2.5 Flash (analysis & Q&A), Gemini `gemini-embedding-001` (embeddings) |
| Streaming  | Server-Sent Events (SSE)                                                   |

---

## 📁 Project Structure

```
papersage/
├── papersage_backend/        # Spring Boot REST API
│   ├── src/main/java/...     #   Controllers, services, DTOs, config
│   ├── pom.xml               #   Maven build
│   └── README.md             #   Backend documentation
│
├── papersage_frontend/       # React SPA
│   ├── src/                  #   Pages, components, API layer
│   ├── package.json          #   npm build
│   └── README.md             #   Frontend documentation
│
└── README.md                 # ← You are here
```

---

## 🚀 Quick Start

### Prerequisites

| Tool         | Version |
| ------------ | ------- |
| Java (JDK)   | 21+     |
| Node.js      | 18+     |
| npm           | 9+      |
| Gemini API Key | [Get one here](https://aistudio.google.com/apikey) |

### 1. Clone the repository

```bash
git clone https://github.com/<your-username>/papersage.git
cd papersage
```

### 2. Start the backend

```bash
cd papersage_backend

# Set your Gemini API key (choose one):

#   Option A – environment variable
export GEMINI_API_KEY=your-key-here        # Linux/macOS
set GEMINI_API_KEY=your-key-here           # Windows CMD

#   Option B – secrets.properties file (recommended)
#   Create src/main/resources/secrets.properties with:
#     gemini.api.key=your-key-here
#   (Already imported by application.yaml via spring.config.import)

./mvnw spring-boot:run          # Linux/macOS
mvnw.cmd spring-boot:run        # Windows
```

The API starts at **http://localhost:8080**.

### 3. Start the frontend

```bash
cd papersage_frontend
npm install

# (Optional) Create .env from the example:
cp .env.example .env
# Default VITE_API_BASE_URL is http://localhost:8080

npm run dev
```

The app opens at **http://localhost:5173**.

---

## 📡 API Overview

All endpoints are under the base path `/api/v1/papers`.

| Method | Endpoint                       | Description                                  |
| ------ | ------------------------------ | -------------------------------------------- |
| `POST` | `/api/v1/papers`               | Upload a PDF → returns structured analysis   |
| `GET`  | `/api/v1/papers/progress`      | SSE stream of pipeline progress events       |
| `POST` | `/api/v1/papers/ask?question=` | Ask a question → grounded answer with sources |
| `POST` | `/api/v1/papers/query?question=` | Retrieve top matching text chunks            |

> See the [Backend README](./papersage_backend/README.md) for full API reference with request/response schemas.

---

## 📚 Further Reading

- [**Backend README**](./papersage_backend/README.md) — API reference, DTOs, services, configuration, error handling
- [**Frontend README**](./papersage_frontend/README.md) — Components, pages, environment variables, build commands

---

## 📝 License

This project is for educational and personal use. See the repository for license details.
