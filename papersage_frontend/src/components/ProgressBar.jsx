import { useEffect, useRef, useState } from 'react'

const STAGE_LABELS = {
  extracting: '📄 Extracting text',
  chunking:   '✂️  Splitting into chunks',
  embedding:  '🔢 Generating embeddings',
  analyzing:  '🤖 Analyzing with AI',
  done:       '✅ Complete',
}

/**
 * Animated progress bar shown during the paper upload pipeline.
 *
 * Props:
 *   percent  {number}  0–100
 *   message  {string}  human-readable stage message from the backend
 *   stage    {string}  stage key (extracting | chunking | embedding | analyzing | done)
 */
const ProgressBar = ({ percent = 0, message = 'Processing…', stage = '' }) => {
  const [elapsed, setElapsed] = useState(0)
  const startRef = useRef(Date.now())

  // Elapsed-time ticker — resets when the component mounts
  useEffect(() => {
    startRef.current = Date.now()
    const id = setInterval(() => {
      setElapsed(Math.floor((Date.now() - startRef.current) / 1000))
    }, 1000)
    return () => clearInterval(id)
  }, [])

  const clampedPct = Math.min(100, Math.max(0, percent))
  const stageLabel = STAGE_LABELS[stage] ?? '⚙️  Processing'

  const mins = Math.floor(elapsed / 60)
  const secs = elapsed % 60
  const elapsedStr = mins > 0
    ? `${mins}m ${secs}s`
    : `${secs}s`

  return (
    <div className="py-8 space-y-4" role="status" aria-live="polite" aria-label={message}>
      {/* Stage label + elapsed timer */}
      <div className="flex items-center justify-between text-sm">
        <span className="font-medium text-[#003057]">{stageLabel}</span>
        <span className="text-slate-400 tabular-nums">{elapsedStr}</span>
      </div>

      {/* Progress bar track */}
      <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden">
        <div
          className="h-2.5 rounded-full bg-[#003057] transition-all duration-500 ease-out"
          style={{ width: `${clampedPct}%` }}
          aria-valuenow={clampedPct}
          aria-valuemin={0}
          aria-valuemax={100}
          role="progressbar"
        />
      </div>

      {/* Percentage + message */}
      <div className="flex items-center justify-between text-xs text-slate-500">
        <span className="truncate max-w-xs">{message}</span>
        <span className="ml-3 tabular-nums font-semibold text-[#B3A369] shrink-0">
          {clampedPct}%
        </span>
      </div>

      {/* Subtle hint about timing */}
      {elapsed >= 5 && clampedPct < 100 && (
        <p className="text-center text-xs text-slate-400 pt-1">
          Large papers can take 20–30 seconds for embedding. Hang tight!
        </p>
      )}
    </div>
  )
}

export default ProgressBar
