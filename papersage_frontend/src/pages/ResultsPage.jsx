import SummarySection from '../components/SummarySection.jsx'
import GlossaryTable from '../components/GlossaryTable.jsx'
import AskSection from '../components/AskSection.jsx'
import PrerequisiteSection from '../components/PrerequisiteSection.jsx'

/**
 * Results page — displays the structured paper analysis and Ask-the-Paper section.
 * Receives analysis data as props; calls onReset to go back to the upload page.
 */
const ResultsPage = ({ analysis, onReset }) => {
  const { executiveSummary = [], keyContributions = [], glossary = [], prerequisiteKnowledge = null } = analysis

  // Use the first executive summary bullet as the TL;DR lead
  const tldr = executiveSummary[0] || null
  const summaryRest = executiveSummary.slice(1)

  return (
    <div className="min-h-screen bg-gradient-to-br from-white to-[#F7F5EE]">
      {/* Header */}
      <header className="border-b border-slate-200 bg-white/90 backdrop-blur-sm sticky top-0 z-10">
        <div className="max-w-3xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <span className="text-2xl" aria-hidden="true">🧠</span>
            <span className="text-xl font-bold text-[#003057] tracking-tight">PaperSage</span>
          </div>
          <button
            onClick={onReset}
            className="text-sm font-medium text-[#B3A369] hover:text-[#9A8C58] hover:underline transition-colors focus:outline-none focus:ring-2 focus:ring-[#B3A369] rounded"
          >
            ← Upload New Paper
          </button>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-3xl mx-auto px-4 sm:px-6 py-10 space-y-6">
        {/* Before You Read */}
        <PrerequisiteSection prerequisiteKnowledge={prerequisiteKnowledge} />

        {/* TL;DR Banner */}
        {tldr && (
          <section
            className="bg-[#003057] rounded-2xl px-6 py-5 text-white shadow-md"
            aria-label="TL;DR"
          >
            <p className="text-xs font-semibold uppercase tracking-widest text-[#B3A369] mb-2">
              TL;DR
            </p>
            <p className="text-base font-medium leading-relaxed">{tldr}</p>
          </section>
        )}

        {/* Executive Summary (remaining bullets) */}
        {summaryRest.length > 0 && (
          <SummarySection
            title="Executive Summary"
            icon="📋"
            items={summaryRest}
          />
        )}

        {/* Key Contributions */}
        <SummarySection
          title="Key Contributions"
          icon="🔬"
          items={keyContributions}
        />

        {/* Glossary */}
        <GlossaryTable entries={glossary} />

        {/* Ask the Paper */}
        <AskSection />
      </main>
    </div>
  )
}

export default ResultsPage
