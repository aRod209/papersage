# Project Brief — PaperSage

## Overview
PaperSage is an AI-powered research paper summarization and Q&A service. The backend is built with Spring Boot and the Google Gemini API. Users upload CS research paper PDFs and receive structured, actionable analysis — plus the ability to ask semantic questions about the paper's content. A React/Vite frontend provides the full UI.

## Core Requirements
1. **PDF Upload & Text Extraction** — Accept PDF files (up to 50MB), extract raw text using Apache PDFBox.
2. **CS Guardrail Classification** — Before expensive processing, use Gemini to verify the document is a CS research paper (HTTP 422 if not).
3. **Structured Paper Analysis** — Use Gemini 2.5 Flash to generate:
   - Executive summary (5–8 bullet points)
   - Key contributions (3–7 bullet points)
   - Glossary of important terms (5–15 entries with plain-language definitions)
   - Prerequisite knowledge (math topics + AI/ML topics a reader should know)
4. **Semantic Q&A** — Chunk the extracted text, embed chunks using Gemini embeddings, and allow users to ask natural-language questions that retrieve the most relevant chunks via cosine similarity.
5. **Real-time Progress** — SSE stream from backend to frontend that shows per-stage upload pipeline progress.

## Key Goals
- Provide a clean REST API consumable by a React frontend
- Return consistent JSON responses (including structured error responses)
- Keep the architecture simple: in-memory storage, no external database
- Leverage Google Gemini for both generative AI (summaries) and embeddings (retrieval)

## Scope
- **In scope**: Backend REST API, PDF processing, AI analysis, semantic retrieval, React frontend UI
- **Out of scope (for now)**: Authentication, persistent storage, multi-paper sessions

## Project Coordinates
- **Group**: `com.anthonyrodriguez`
- **Artifact**: `papersage_backend`
- **Version**: `0.0.1-SNAPSHOT`
- **Java**: 21
- **Spring Boot**: 3.5.11
