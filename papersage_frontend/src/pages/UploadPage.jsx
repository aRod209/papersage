import { useRef, useState } from 'react'
import { uploadPaper } from '../api/paperApi.js'
import UploadDropzone from '../components/UploadDropzone.jsx'
import ProgressBar from '../components/ProgressBar.jsx'

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

/**
 * Upload page — lets the user select and upload a PDF.
 * On success, calls onSuccess(analysisData) to transition to the Results page.
 *
 * While the upload pipeline runs, an SSE stream from /api/v1/papers/progress
 * pushes per-stage progress events that are shown in the ProgressBar.
 */
const UploadPage = ({ onSuccess }) => {
  const [file, setFile] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [progress, setProgress] = useState({ stage: '', message: 'Preparing…', percent: 0 })

  const eventSourceRef = useRef(null)

  /** Opens the SSE stream before submitting the upload. */
  const openProgressStream = () => {
    // Close any pre-existing stream
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
    }

    const es = new EventSource(`${API_BASE}/api/v1/papers/progress`)
    eventSourceRef.current = es

    es.addEventListener('progress', (e) => {
      try {
        const data = JSON.parse(e.data)
        setProgress({
          stage:   data.stage   ?? '',
          message: data.message ?? 'Processing…',
          percent: data.percent ?? 0,
        })
      } catch {
        // ignore malformed events
      }
    })

    es.addEventListener('error', () => {
      // SSE transport error — don't surface to user; the POST response will handle it
      es.close()
    })
  }

  /** Closes and discards the SSE stream. */
  const closeProgressStream = () => {
    if (eventSourceRef.current) {
      eventSourceRef.current.close()
      eventSourceRef.current = null
    }
  }

  const handleUpload = async () => {
    if (!file) return

    setLoading(true)
    setError(null)
    setProgress({ stage: '', message: 'Connecting…', percent: 0 })

    // Open SSE stream first so it is registered before the POST fires
    openProgressStream()

    // Small delay to let the SSE handshake complete before the backend starts
    await new Promise((resolve) => setTimeout(resolve, 150))

    try {
      const data = await uploadPaper(file)
      closeProgressStream()
      onSuccess(data)
    } catch (err) {
      closeProgressStream()
      setError(err.message || 'Upload failed. Please try again.')
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-white to-[#F7F5EE] flex flex-col">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white/90 backdrop-blur-sm">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 py-4 flex items-center gap-3">
          <span className="text-2xl" aria-hidden="true">🧠</span>
          <span className="text-xl font-bold text-[#003057] tracking-tight">PaperSage</span>
          <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-[#003057] text-[#B3A369] ml-1">
            AI Research Assistant
          </span>
        </div>
      </header>

      {/* Main */}
      <main className="flex-1 flex items-center justify-center px-4 sm:px-6 py-16">
        <div className="w-full max-w-lg space-y-6">
          <div className="text-center space-y-2">
            <h1 className="text-3xl font-bold text-[#003057] tracking-tight">
              Understand any research paper
            </h1>
            <p className="text-slate-500 text-base">
              Upload a PDF and get an AI-powered summary, key contributions, glossary, and Q&amp;A — in seconds.
            </p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-5">
            <UploadDropzone onFileSelect={setFile} disabled={loading} />

            {error && (
              <div
                className="rounded-xl bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-700 flex items-start gap-2"
                role="alert"
              >
                <span aria-hidden="true" className="mt-0.5">⚠️</span>
                <span>{error}</span>
              </div>
            )}

            {loading ? (
              <ProgressBar
                percent={progress.percent}
                message={progress.message}
                stage={progress.stage}
              />
            ) : (
              <button
                onClick={handleUpload}
                disabled={!file}
                className="w-full py-3 rounded-xl bg-[#003057] text-white font-semibold text-sm hover:bg-[#002244] active:bg-[#001833] disabled:opacity-40 disabled:cursor-not-allowed transition-colors focus:outline-none focus:ring-2 focus:ring-[#B3A369] focus:ring-offset-2"
              >
                Analyze Paper
              </button>
            )}
          </div>

          <p className="text-center text-xs text-slate-400">
            Supports PDF files up to 50 MB · No data is stored after your session
          </p>
        </div>
      </main>
    </div>
  )
}

export default UploadPage
