/**
 * PrerequisiteSection — displays focused background knowledge topics a reader
 * should know before reading the paper. Split into Math and AI/ML categories.
 *
 * Props:
 *   prerequisiteKnowledge: { mathTopics: string[], aiMlTopics: string[] } | null | undefined
 */
const PrerequisiteSection = ({ prerequisiteKnowledge }) => {
  const mathTopics = prerequisiteKnowledge?.mathTopics ?? []
  const aiMlTopics = prerequisiteKnowledge?.aiMlTopics ?? []

  return (
    <section
      className="bg-white rounded-2xl border border-slate-200 shadow-sm px-6 py-5"
      aria-label="Before You Read"
    >
      <div className="flex items-center gap-2 mb-4">
        <span className="text-xl" aria-hidden="true">📚</span>
        <h2 className="text-base font-semibold text-[#003057]">Before You Read</h2>
      </div>

      <p className="text-sm text-slate-500 mb-4">
        Helpful background knowledge for this paper:
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
        {/* Math Topics */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-widest text-[#9A8C58] mb-2 flex items-center gap-1">
            <span aria-hidden="true">📐</span> Math
          </h3>
          {mathTopics.length > 0 ? (
            <ul className="space-y-1">
              {mathTopics.map((topic) => (
                <li key={topic} className="flex items-start gap-2 text-sm text-slate-700">
                  <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#B3A369] shrink-0" aria-hidden="true" />
                  {topic}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-400 italic">None identified</p>
          )}
        </div>

        {/* AI/ML Topics */}
        <div>
          <h3 className="text-xs font-semibold uppercase tracking-widest text-[#9A8C58] mb-2 flex items-center gap-1">
            <span aria-hidden="true">🤖</span> AI / ML
          </h3>
          {aiMlTopics.length > 0 ? (
            <ul className="space-y-1">
              {aiMlTopics.map((topic) => (
                <li key={topic} className="flex items-start gap-2 text-sm text-slate-700">
                  <span className="mt-1.5 h-1.5 w-1.5 rounded-full bg-[#003057] shrink-0" aria-hidden="true" />
                  {topic}
                </li>
              ))}
            </ul>
          ) : (
            <p className="text-sm text-slate-400 italic">None identified</p>
          )}
        </div>
      </div>
    </section>
  )
}

export default PrerequisiteSection
