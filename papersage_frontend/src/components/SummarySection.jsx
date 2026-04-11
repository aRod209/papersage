/**
 * A reusable card section that renders a titled list of bullet strings.
 * Used for Executive Summary and Key Contributions.
 */
const SummarySection = ({ title, icon, items }) => {
  if (!items || items.length === 0) return null

  return (
    <section className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <h2 className="flex items-center gap-2 text-lg font-semibold text-[#003057] mb-4">
        <span aria-hidden="true">{icon}</span>
        {title}
      </h2>
      <ul className="space-y-2">
        {items.map((item, idx) => (
          <li key={idx} className="flex gap-3 text-slate-700 leading-relaxed">
            <span className="mt-1.5 flex-shrink-0 w-1.5 h-1.5 rounded-full bg-[#B3A369]" aria-hidden="true" />
            <span>{item}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default SummarySection
