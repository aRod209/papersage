---
paths:
  - "papersage_frontend/src/**"    # All files under src/
---

# PaperSage Frontend — Cline Rules

You are an expert frontend software developer specializing in React and Vite development for web APIs.

## Project Overview
- **App**: PaperSage — AI-powered research paper analysis frontend
- **Framework**: React 18+ with TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: TanStack Query (React Query) for server state, React useState/useContext for UI state
- **Backend**: Spring Boot REST API at `/api/v1/papers`

## API Contract
The frontend consumes the PaperSage Backend REST API:
- `POST /api/v1/papers` — Upload PDF, returns `PaperAnalysisResponse` (executiveSummary, keyContributions, glossary)
- `POST /api/v1/papers/query` — Semantic retrieval, returns `QueryResponse` (question, topChunks)
- `POST /api/v1/papers/ask` — Grounded Q&A, returns `AnswerResponse` (question, answer, sources)

All error responses follow: `{ "error": string, "message": string }`

## TypeScript Types
Define types matching the backend DTOs exactly:
- `PaperAnalysisResponse` — { executiveSummary: string[], keyContributions: string[], glossary: GlossaryEntry[] }
- `GlossaryEntry` — { term: string, definition: string }
- `RetrievalResult` — { chunk: TextChunk, similarityScore: number }
- `TextChunk` — { chunkId: string, chunkText: string, chunkIndex: number, sectionLabel: string }
- `QueryResponse` — { question: string, topChunks: RetrievalResult[] }
- `AnswerResponse` — { question: string, answer: string, sources: SourceReference[] }
- `SourceReference` — { chunkId: string, chunkIndex: number, sectionLabel: string, similarityScore: number }
- `ErrorResponse` — { error: string, message: string }

## Code Standards
- Use TypeScript (`.tsx` / `.ts`) for ALL files — no `.js` / `.jsx`
- Use functional components with hooks only — no class components
- Use `const` arrow functions for components: `const MyComponent: React.FC<Props> = () => {}`
- Define prop interfaces as `interface MyComponentProps {}` above the component
- Prefer named exports over default exports
- Keep components under 150 lines — extract sub-components when exceeding this
- No `any` type — use `unknown` if the type is truly unknown
- No `console.log` in committed code
- Use early returns for conditional rendering instead of nested ternaries

## File & Folder Structure
```
src/
├── api/                    # API client and endpoint functions
│   ├── client.ts           # Axios instance with base URL and interceptors
│   └── papers.ts           # Paper-related API calls (upload, query, ask)
├── components/             # Shared/reusable UI components
│   ├── ui/                 # Primitives (Button, Card, Input, Spinner, etc.)
│   └── layout/             # Layout components (Header, Footer, PageContainer)
├── features/               # Feature modules
│   ├── upload/             # PDF upload feature
│   │   ├── components/
│   │   └── hooks/
│   ├── analysis/           # Paper analysis display (summary, contributions, glossary)
│   │   ├── components/
│   │   └── hooks/
│   └── chat/               # Q&A / chat interface
│       ├── components/
│       └── hooks/
├── hooks/                  # Shared custom hooks
├── types/                  # Shared TypeScript types (matching backend DTOs)
│   └── api.ts
├── utils/                  # Pure utility functions
├── styles/                 # Global styles
├── App.tsx
├── main.tsx
└── vite-env.d.ts
```

## Component Guidelines
- Every component in its own file, named with PascalCase
- Colocate component-specific hooks in the same feature folder
- Handle loading, error, and empty states in EVERY data-fetching component
- Use Suspense boundaries where appropriate
- Accessibility: use semantic HTML, proper ARIA attributes, keyboard navigation

## Naming Conventions
- Components: `PascalCase.tsx` (e.g., `UploadDropzone.tsx`)
- Hooks: `camelCase` with `use` prefix (e.g., `useUploadPaper.ts`)
- Types: `PascalCase` (e.g., `PaperAnalysisResponse`)
- Utilities: `camelCase.ts` (e.g., `formatScore.ts`)
- Constants: `UPPER_SNAKE_CASE`
- CSS classes: Tailwind utilities; custom classes in `kebab-case`

## Data Fetching & State
- Use TanStack Query (React Query) for ALL server state (uploads, queries, Q&A)
- Define custom hooks wrapping `useMutation` / `useQuery` (e.g., `useUploadPaper`, `useAskQuestion`)
- API base URL from Vite env var: `VITE_API_BASE_URL`
- Never store API response data in React state — let TanStack Query cache handle it
- Local UI state (modals, form inputs, toggles) → `useState`

## Styling
- Tailwind CSS as the primary styling approach
- Mobile-first responsive design
- Dark mode support via Tailwind `dark:` variants
- Use a consistent color palette defined in `tailwind.config.ts`
- No inline styles except for truly dynamic values

## Performance
- Lazy load routes with `React.lazy()` + `Suspense`
- Optimize PDF upload UX with progress indicators
- Debounce Q&A input (300ms) before sending API requests
- Virtualize long chunk lists if needed (react-window or similar)

## Testing
- Unit tests for utility functions and custom hooks
- Component tests with React Testing Library (test behavior, not implementation)
- Use MSW (Mock Service Worker) for API mocking
- Test files colocated: `Component.tsx` → `Component.test.tsx`

## Error Handling
- Global error boundary at app root
- API errors: parse `ErrorResponse` from backend and display user-friendly messages
- Upload errors: handle 413 (file too large), 400 (bad request), 503 (Gemini unavailable)
- Network errors: show retry UI

## Git & Workflow
- Conventional commits: `feat:`, `fix:`, `refactor:`, `docs:`, `test:`, `chore:`
- Run `npm run lint` and `npm run typecheck` before committing
- No committed `.env` files — use `.env.example` as a template

## Environment Variables
```
VITE_API_BASE_URL=http://localhost:8080
```
