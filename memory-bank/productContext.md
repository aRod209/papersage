# Product Context — PaperSage

## Why This Project Exists
Reading and understanding dense computer science research papers is time-consuming and cognitively demanding. Researchers, students, and developers often need to quickly grasp a paper's key ideas, contributions, and terminology without reading every page in detail.

## Problems It Solves
1. **Information overload** — CS papers are often 10–30+ pages of dense technical content. PaperSage distills them into concise, structured summaries.
2. **Vocabulary barrier** — Papers use specialized jargon. The glossary feature provides plain-language definitions of important terms.
3. **Prerequisite gap** — Readers don't always know what math or AI/ML background they need. The "Before You Read" section lists exactly that.
4. **Targeted retrieval** — Instead of re-reading entire papers, users can ask specific questions and get the most relevant passages back via semantic search.
5. **Non-CS upload risk** — Without a guardrail, users might accidentally upload resumes or biology papers. The CS guardrail classifier rejects non-CS documents immediately with a clear 422 error.

## How It Works (User Flow)
1. **Upload**: User uploads a PDF of a CS research paper via the UI (drag-and-drop or file picker).
2. **Progress**: A real-time SSE progress bar shows each stage: extracting → classifying → chunking → embedding → analyzing.
3. **Guardrail**: Backend sends the first 3000 characters to Gemini at temperature 0.0 for a YES/NO CS classification. Non-CS documents are rejected (HTTP 422).
4. **Analysis**: The backend extracts text, chunks it, embeds it, and sends it to Gemini for structured analysis.
5. **Response**: User receives a structured results page:
   - **Before You Read** — prerequisite math and AI/ML topics
   - **TL;DR** banner — first executive summary bullet promoted as a lead
   - **Executive Summary** — remaining bullets
   - **Key Contributions**
   - **Glossary**
6. **Q&A**: User can ask follow-up questions. The system embeds the question, computes cosine similarity against stored chunk embeddings, and returns the top-5 most relevant passages with a grounded AI answer.

## User Experience Goals
- **Fast, structured insights** — No need to read the full paper to understand its core ideas.
- **Plain-language accessibility** — Summaries and glossary entries are written for clarity, not academic formality.
- **Real-time feedback** — The SSE progress bar shows exactly which pipeline stage is running and how far along it is.
- **Semantic precision** — Q&A retrieval finds the most relevant sections, not just keyword matches.
- **Consistent API contract** — Clean JSON responses with predictable error handling for seamless frontend integration.
- **Georgia Tech brand** — UI uses official GT Navy (#003057) and Tech Gold (#B3A369) throughout.

## Target Audience
- CS researchers conducting literature reviews
- Students studying research papers
- Developers exploring academic concepts for practical application
