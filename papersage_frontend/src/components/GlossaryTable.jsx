/**
 * Renders the glossary as a clean two-column definition table.
 */
const GlossaryTable = ({ entries }) => {
  if (!entries || entries.length === 0) return null

  return (
    <section className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
      <h2 className="flex items-center gap-2 text-lg font-semibold text-[#003057] mb-4">
        <span aria-hidden="true">📖</span>
        Glossary
      </h2>
      <dl className="divide-y divide-slate-100">
        {entries.map((entry, idx) => (
          <div key={idx} className="py-3 sm:grid sm:grid-cols-3 sm:gap-4">
            <dt className="text-sm font-semibold text-[#003057] flex items-start pt-0.5">
              {entry.term}
            </dt>
            <dd className="mt-1 text-sm text-slate-600 sm:mt-0 sm:col-span-2 leading-relaxed">
              {entry.definition}
            </dd>
          </div>
        ))}
      </dl>
    </section>
  )
}

export default GlossaryTable
