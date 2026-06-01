# PaperSage RAG Implementation Documentation

## Purpose

This documentation set explains PaperSage's Retrieval-Augmented Generation (RAG) implementation as it exists in the current brownfield codebase. It is written as an AWS-AIDLC reverse-engineering artifact so future work on chunking, embeddings, retrieval, answer generation, persistence, or multi-session support can start from a clear source-of-truth overview.

PaperSage uses RAG to let users ask natural-language questions about the currently uploaded computer science research paper. The implementation combines upload-time indexing with ask-time retrieval and grounded answer generation.

## Documentation Map

| Document | Purpose |
|---|---|
| [rag-for-beginners.md](./rag-for-beginners.md) | Plain-language introduction to RAG and how PaperSage implements it. |
| [architecture.md](./architecture.md) | Component-level RAG architecture, responsibilities, and system boundaries. |
| [indexing-pipeline.md](./indexing-pipeline.md) | Upload-time extraction, guardrail, chunking, document embedding, and in-memory indexing flow. |
| [ask-pipeline.md](./ask-pipeline.md) | Ask-time query embedding, top-k retrieval, grounded prompt construction, answer generation, and source references. |
| [data-contracts.md](./data-contracts.md) | REST endpoints, DTOs, prompt contracts, and frontend integration points. |
| [operations-and-limitations.md](./operations-and-limitations.md) | Runtime characteristics, configuration, failure handling, MVP limitations, and production-readiness recommendations. |

## RAG at a Glance

PaperSage's RAG implementation has two main phases:

1. **Indexing phase, during paper upload**
   - Extract PDF text with Apache PDFBox.
   - Verify the upload is a CS research paper before expensive processing.
   - Split extracted text into overlapping chunks.
   - Generate Gemini document embeddings for each chunk.
   - Store chunk/vector pairs in an in-memory index.
2. **Ask phase, when the user asks a question**
   - Embed the question with Gemini using retrieval-query task hints.
   - Score all indexed chunks with cosine similarity.
   - Select the top 5 chunks.
   - Insert those chunks into a grounded answer prompt.
   - Ask Gemini 2.5 Flash to answer using only the supplied context.
   - Return the answer plus source references.

## Primary Source Traceability

| Concern | Source file |
|---|---|
| HTTP upload, query, ask, and SSE orchestration | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/controller/PaperController.java` |
| Text chunking | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/TextChunkingService.java` |
| Gemini embeddings | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/GeminiEmbeddingService.java` |
| In-memory vector retrieval | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/SemanticRetrievalService.java` |
| Grounded answer generation | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/GroundedAnswerService.java` |
| Grounded answer prompt template | `papersage_backend/src/main/resources/prompts/grounded-answer.txt` |
| Embedding resilience settings | `papersage_backend/src/main/resources/application.yaml` |
| Frontend API calls | `papersage_frontend/src/api/paperApi.js` |
| Frontend ask UI | `papersage_frontend/src/components/AskSection.jsx` |

## Current Architecture Summary

The current implementation is intentionally simple and MVP-oriented:

- **Storage:** in-memory only; no database or vector store.
- **Session model:** single-paper session; a new upload replaces the previous index.
- **Retrieval strategy:** brute-force cosine similarity over all indexed chunks.
- **Embedding provider:** Gemini `gemini-embedding-001`.
- **Generation provider:** Gemini `gemini-2.5-flash`.
- **Grounding strategy:** prompt-level grounding using retrieved chunks and explicit instructions not to use outside knowledge.

## Recommended Reading Order

1. If you are new to RAG, start with [rag-for-beginners.md](./rag-for-beginners.md).
2. Then read [architecture.md](./architecture.md).
3. Read [indexing-pipeline.md](./indexing-pipeline.md) to understand how the retrieval corpus is built.
4. Read [ask-pipeline.md](./ask-pipeline.md) to understand runtime RAG behavior.
5. Use [data-contracts.md](./data-contracts.md) as the API and DTO reference.
6. Use [operations-and-limitations.md](./operations-and-limitations.md) before refactoring or production hardening.
