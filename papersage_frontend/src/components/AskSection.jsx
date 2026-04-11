import { useState } from 'react'
import { askQuestion } from '../api/paperApi.js'
import SourceBadge from './SourceBadge.jsx'
import LoadingSpinner from './LoadingSpinner.jsx'

/**
 * Ask-the-Paper section.
 * Manages its own local state: question input, answer, sources, loading, error.
 */
const AskSection = () => {
  const [question, setQuestion] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    const trimmed = question.trim()
    if (!trimmed) return

    setLoading(true)
    setError(null)
    setResult(null)

    try {
      const data = await askQuestion(trimmed)
      setResult(data)
    } catch (err) {
      setError(err.message || 'Something went wrong. Please try again.')
    } finally {
      setLoading(false)
    }
  }

  const canSubmit = question.trim().length > 0 && !loading

  return (
    <section className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-5">
      <h2 className="flex items-center gap-2 text-lg font-semibold text-[#003057]">
        <span aria-hidden="true">🤖</span>
        Ask the Paper
      </h2>

      <form onSubmit={handleSubmit} className="flex gap-3">
        <label htmlFor="question-input" className="sr-only">
          Ask a question about the paper
        </label>
        <input
          id="question-input"
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="e.g. What is the main contribution of this paper?"
          disabled={loading}
          className="flex-1 rounded-xl border border-slate-300 px-4 py-2.5 text-sm text-slate-800 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-[#B3A369] focus:border-transparent disabled:opacity-50 transition"
        />
        <button
          type="submit"
          disabled={!canSubmit}
          className="px-5 py-2.5 rounded-xl bg-[#003057] text-white text-sm font-semibold hover:bg-[#002244] active:bg-[#001833] disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          Ask
        </button>
      </form>

      {loading && <LoadingSpinner label="Thinking…" />}

      {error && (
        <div
          className="rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-start gap-2"
          role="alert"
        >
          <span aria-hidden="true" className="mt-0.5">⚠️</span>
          <span>{error}</span>
        </div>
      )}

      {result && !loading && (
        <div className="space-y-4">
          <div className="rounded-xl bg-[#F7F5EE] border border-[#B3A369]/30 px-5 py-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-[#9A8C58] mb-2">Answer</p>
            <p className="text-slate-800 leading-relaxed text-sm">{result.answer}</p>
          </div>

          {result.sources && result.sources.length > 0 && (
            <div>
              <p className="text-xs font-semibold uppercase tracking-wide text-[#9A8C58] mb-2">Sources</p>
              <div className="flex flex-wrap gap-2">
                {result.sources.map((source) => (
                  <SourceBadge key={source.chunkId} source={source} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </section>
  )
}

export default AskSection
