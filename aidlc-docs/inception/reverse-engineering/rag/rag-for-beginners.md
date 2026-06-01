# RAG for Beginners in PaperSage

## Who this guide is for

This guide is for readers who are new to **RAG** and want a plain-language explanation of what PaperSage is doing when it answers questions about an uploaded paper.

If you already understand embeddings, vector search, cosine similarity, and prompt grounding, the deeper technical documents in this folder will be more useful. If those terms are unfamiliar, start here.

## What RAG means

**RAG** stands for **Retrieval-Augmented Generation**.

That sounds complicated, but the idea is simple:

1. **Retrieve** the most relevant information from a source document.
2. **Augment** the AI prompt with that retrieved information.
3. **Generate** an answer using the retrieved context.

In other words, RAG is a way to help an AI answer questions by first giving it the most useful pieces of information from a trusted source.

## A simple analogy

Imagine you ask a friend a question about a long research paper.

Without RAG, your friend might try to answer from memory or general knowledge.

With RAG, you first highlight the few paragraphs that seem most relevant, hand those paragraphs to your friend, and say:

> Please answer using these highlighted parts of the paper.

That is roughly what PaperSage does. It finds the parts of the uploaded paper that best match your question, then asks Gemini to answer using those parts.

## Why PaperSage uses RAG

PaperSage uses RAG because research papers are long and user questions are specific.

For example, a user might ask:

- “What dataset did the authors use?”
- “What is the main contribution?”
- “How did they evaluate the model?”
- “What are the limitations?”

Instead of asking Gemini to answer from the whole application state or from general knowledge, PaperSage retrieves the most relevant chunks from the uploaded paper and uses those chunks as the answer context.

This helps PaperSage:

- focus on the uploaded paper,
- return answers that are easier to trace back to source text,
- avoid relying only on the model's general training knowledge,
- provide source references with answers.

## How PaperSage implements RAG

PaperSage's RAG flow has two main parts:

1. what happens when a paper is uploaded,
2. what happens when a user asks a question.

### Part 1: When a paper is uploaded

When you upload a PDF, PaperSage prepares the paper for future questions.

1. **Extract text from the PDF**
   - `PdfExtractionService` reads the PDF using Apache PDFBox.
   - The output is plain text from the paper.

2. **Check that the document is a CS research paper**
   - `PaperGuardrailService` asks Gemini to classify the document.
   - If the document does not look like a computer science research paper, PaperSage rejects it before doing expensive processing.

3. **Split the paper into chunks**
   - `TextChunkingService` breaks the paper into smaller overlapping text sections.
   - This matters because it is easier to search small sections than one huge document.

4. **Create embeddings for each chunk**
   - `GeminiEmbeddingService` sends each chunk to Gemini's embedding model, `gemini-embedding-001`.
   - An embedding is a list of numbers that represents the meaning of text.

5. **Store the chunks and embeddings in memory**
   - `SemanticRetrievalService` keeps the chunks and their embeddings in an in-memory index.
   - In the current MVP, this index is not saved to a database. Restarting the backend clears it.

After this upload process, PaperSage has a searchable version of the paper ready for questions.

### Part 2: When a user asks a question

When you ask a question, PaperSage uses the prepared chunks to find the best context.

1. **Embed the question**
   - `GeminiEmbeddingService` converts the user's question into an embedding.
   - This puts the question into the same kind of meaning-based number format as the paper chunks.

2. **Compare the question to the paper chunks**
   - `SemanticRetrievalService` compares the question embedding with each chunk embedding.
   - It uses cosine similarity to estimate which chunks are closest in meaning to the question.

3. **Retrieve the top matching chunks**
   - PaperSage selects the most relevant chunks, currently the top 5.
   - These chunks become the context for the answer.

4. **Ask Gemini to answer with that context**
   - `GroundedAnswerService` builds a prompt containing the retrieved chunks and the user's question.
   - Gemini 2.5 Flash generates an answer based on that supplied context.

5. **Return the answer with sources**
   - PaperSage returns the generated answer and lightweight source references.
   - Each source reference points back to a retrieved chunk and includes metadata such as section label and similarity score.

## Important beginner terms

### Chunk

A **chunk** is a smaller section of the paper text.

PaperSage chunks the paper because searching smaller sections is more practical than searching one large document all at once.

### Embedding

An **embedding** is a numeric representation of text meaning.

Texts with similar meanings usually have embeddings that are close to each other mathematically.

For example, a question about “training data” should be closer to chunks that discuss datasets or experiments than to chunks that discuss unrelated background material.

### Semantic retrieval

**Semantic retrieval** means searching by meaning rather than exact keywords.

This is useful because a question and a paper passage may use different words while still discussing the same idea.

### Cosine similarity

**Cosine similarity** is the scoring method PaperSage uses to compare embeddings.

In simple terms, it helps decide which paper chunks are closest in meaning to the user's question.

### Grounded answer

A **grounded answer** is an answer generated from supplied source context.

In PaperSage, the source context is the set of retrieved paper chunks.

## What RAG helps with in PaperSage

RAG helps PaperSage answer questions that are tied to the uploaded paper, such as:

- what the paper claims,
- what methods it uses,
- what experiments it reports,
- what limitations it mentions,
- what terms or concepts appear in the paper.

RAG is especially helpful when the answer is located in one part of a long paper and the user does not know where to look.

## What RAG does not guarantee

RAG improves grounding, but it is not magic.

Current limitations include:

- **PDF extraction quality matters.** If the PDF text is not extracted correctly, retrieval quality can suffer.
- **Only indexed text can be retrieved.** If a detail is missing from the extracted chunks, PaperSage may not find it.
- **The current index is in memory only.** PaperSage forgets indexed chunks when the backend restarts.
- **The current MVP is single-paper oriented.** Uploading a new paper replaces the previous paper's indexed chunks.
- **Answers still come from an LLM.** PaperSage grounds the prompt with retrieved chunks, but generated answers should still be reviewed for accuracy.

## Where to look in the code

The main RAG implementation lives in the backend:

| Concern | Source file |
|---|---|
| Upload, query, ask endpoint orchestration | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/controller/PaperController.java` |
| Splitting paper text into chunks | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/TextChunkingService.java` |
| Creating document and question embeddings | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/GeminiEmbeddingService.java` |
| Storing chunks and retrieving relevant matches | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/SemanticRetrievalService.java` |
| Building grounded prompts and generating answers | `papersage_backend/src/main/java/com/anthonyrodriguez/papersage_backend/service/GroundedAnswerService.java` |
| Grounded answer prompt template | `papersage_backend/src/main/resources/prompts/grounded-answer.txt` |

The frontend calls the RAG-powered ask endpoint from:

| Concern | Source file |
|---|---|
| API call for asking questions | `papersage_frontend/src/api/paperApi.js` |
| User interface for question and answer flow | `papersage_frontend/src/components/AskSection.jsx` |

## Suggested next reading

After this beginner guide, read the focused technical documents in this order:

1. [architecture.md](./architecture.md)
2. [indexing-pipeline.md](./indexing-pipeline.md)
3. [ask-pipeline.md](./ask-pipeline.md)
4. [data-contracts.md](./data-contracts.md)
5. [operations-and-limitations.md](./operations-and-limitations.md)
