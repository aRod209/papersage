/**
 * A small chip displaying a single source reference from an AnswerResponse.
 * Shows the chunk index, section label, and similarity score.
 */
const SourceBadge = ({ source }) => {
  const score = (source.similarityScore * 100).toFixed(0)
  const label = source.sectionLabel || `Chunk ${source.chunkIndex + 1}`

  return (
    <span
      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-[#003057]/5 text-[#003057] border border-[#003057]/20"
      title={`Chunk ID: ${source.chunkId}`}
    >
      <span className="w-1.5 h-1.5 rounded-full bg-[#B3A369] flex-shrink-0" aria-hidden="true" />
      {label}
      <span className="text-[#B3A369] font-normal">· {score}%</span>
    </span>
  )
}

export default SourceBadge
