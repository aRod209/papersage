# 🖥️ PaperSage Frontend

**React SPA for AI-powered research paper analysis**

[![React 19](https://img.shields.io/badge/React-19-61DAFB?logo=react)](https://react.dev/)
[![Vite 6](https://img.shields.io/badge/Vite-6-646CFF?logo=vite)](https://vite.dev/)
[![Tailwind CSS 4](https://img.shields.io/badge/Tailwind%20CSS-4-06B6D4?logo=tailwindcss)](https://tailwindcss.com/)

---

## Overview

The PaperSage frontend is a single-page application built with **React 19**, **Vite 6**, and **Tailwind CSS 4**. It provides an intuitive interface for uploading CS research papers and exploring AI-generated analysis results.

### User Flow

1. **Upload** — Drag & drop (or click to select) a PDF on the upload page
2. **Progress** — Watch real-time pipeline progress via a live progress bar (SSE-powered)
3. **Results** — Browse the structured analysis: executive summary, key contributions, glossary, and prerequisite knowledge
4. **Ask** — Ask follow-up questions about the paper and receive grounded answers with source citations

---

## 🛠️ Tech Stack

| Technology      | Version | Purpose                          |
| --------------- | ------- | -------------------------------- |
| React           | 19.1    | UI component library             |
| Vite            | 6.3     | Build tool & dev server          |
| Tailwind CSS    | 4.1     | Utility-first CSS framework      |
| JavaScript/JSX  | ES2020  | Language                         |

---

## 📁 Project Structure

```
papersage_frontend/
├── public/
│   ├── favicon.svg               # App favicon
│   └── icons.svg                 # UI icons
│
├── src/
│   ├── main.jsx                  # React entry point (renders <App />)
│   ├── App.jsx                   # Root component — manages view state
│   ├── index.css                 # Global styles (Tailwind imports)
│   │
│   ├── api/
│   │   └── paperApi.js           # API client (upload, ask question)
│   │
│   ├── assets/
│   │   ├── hero.png              # Landing page hero image
│   │   ├── typescript.svg        # Asset
│   │   └── vite.svg              # Asset
│   │
│   ├── components/
│   │   ├── AskSection.jsx        # Q&A input + answer display
│   │   ├── GlossaryTable.jsx     # Term/definition table
│   │   ├── LoadingSpinner.jsx    # Spinner animation
│   │   ├── PrerequisiteSection.jsx # Math & AI/ML topic lists
│   │   ├── ProgressBar.jsx       # Real-time upload progress bar
│   │   ├── SourceBadge.jsx       # Chunk source citation badge
│   │   ├── SummarySection.jsx    # Executive summary & key contributions
│   │   └── UploadDropzone.jsx    # Drag-and-drop PDF upload area
│   │
│   └── pages/
│       ├── UploadPage.jsx        # Landing page with upload dropzone
│       └── ResultsPage.jsx       # Analysis results + Q&A interface
│
├── index.html                    # HTML shell
├── vite.config.js                # Vite configuration
├── tsconfig.json                 # TypeScript/JS config
├── package.json                  # Dependencies & scripts
├── .env.example                  # Example environment variables
└── README.md                     # ← You are here
```

---

## 🧩 Components

### Pages

| Component      | Description                                                                 |
| -------------- | --------------------------------------------------------------------------- |
| `UploadPage`   | Landing page with drag-and-drop upload, SSE progress stream, and error handling. Calls `openProgressStream()` before upload and `closeProgressStream()` on completion. |
| `ResultsPage`  | Displays the full analysis (summary, contributions, glossary, prerequisites) and hosts the Q&A section. Includes a "Reset" action to upload a new paper. |

### UI Components

| Component             | Description                                                        |
| --------------------- | ------------------------------------------------------------------ |
| `UploadDropzone`      | Drag-and-drop zone for PDF files. Validates file type before upload. |
| `ProgressBar`         | Animated progress bar that reflects real-time SSE pipeline events.  |
| `LoadingSpinner`      | Generic loading spinner for async operations.                      |
| `SummarySection`      | Renders executive summary and key contributions as bullet lists.   |
| `GlossaryTable`       | Displays glossary entries in a styled term/definition table.       |
| `PrerequisiteSection` | Shows math and AI/ML prerequisite topics in categorized lists.     |
| `AskSection`          | Text input for asking questions, displays answer + source badges.  |
| `SourceBadge`         | Small badge showing section label and similarity score for a source citation. |

### API Layer

| Function         | Method | Endpoint                        | Description                        |
| ---------------- | ------ | ------------------------------- | ---------------------------------- |
| `uploadPaper(file)` | `POST` | `/api/v1/papers`             | Uploads PDF, returns analysis JSON |
| `askQuestion(question)` | `POST` | `/api/v1/papers/ask?question=` | Asks a question, returns grounded answer |

The SSE progress stream (`GET /api/v1/papers/progress`) is opened directly in `UploadPage` using the `EventSource` API.

---

## ⚙️ Environment Variables

Copy the example file and configure as needed:

```bash
cp .env.example .env
```

| Variable              | Default                  | Description                      |
| --------------------- | ------------------------ | -------------------------------- |
| `VITE_API_BASE_URL`   | `http://localhost:8080`  | Backend API base URL             |

> All environment variables prefixed with `VITE_` are exposed to the client at build time via Vite's `import.meta.env`.

---

## 🚀 Getting Started

### Prerequisites

- **Node.js** 18+ — [Download](https://nodejs.org/)
- **npm** 9+
- Backend running at `http://localhost:8080` (see [Backend README](../papersage_backend/README.md))

### Install & Run

```bash
# 1. Navigate to the frontend directory
cd papersage_frontend

# 2. Install dependencies
npm install

# 3. (Optional) Create .env from the example
cp .env.example .env

# 4. Start the development server
npm run dev
```

The app opens at **http://localhost:5173**.

### Build for Production

```bash
# Build optimized static files
npm run build

# Preview the production build locally
npm run preview
```

The production build outputs to `dist/`.

---

## 📜 Available Scripts

| Script          | Command             | Description                              |
| --------------- | ------------------- | ---------------------------------------- |
| `dev`           | `vite`              | Start development server with HMR        |
| `build`         | `vite build`        | Build optimized production bundle        |
| `preview`       | `vite preview`      | Preview production build locally         |

---

## 🔗 Related

- [**Root README**](../README.md) — Project overview & quick start
- [**Backend README**](../papersage_backend/README.md) — Spring Boot API documentation
