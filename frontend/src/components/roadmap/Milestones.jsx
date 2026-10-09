import { Flag, Award, ExternalLink, CheckCircle2, Calendar } from 'lucide-react'

export default function Milestones({ milestones = [], certifications = [] }) {
  return (
    <div className="space-y-6">
      {/* Milestones Timeline */}
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <Flag className="h-5 w-5 text-emerald-600" />
          <h3 className="section-title mb-0">Career Milestones</h3>
        </div>

        {milestones.length === 0 ? (
          <div className="card p-6 border border-surface-200 text-center">
            <p className="text-xs text-surface-500">No specific career milestones listed.</p>
          </div>
        ) : (
          <div className="relative pl-6 border-l-2 border-emerald-300 space-y-4">
            {milestones.map((m, idx) => (
              <div key={idx} className="relative">
                <div className="absolute -left-[31px] top-1 w-6 h-6 rounded-full bg-emerald-600 text-white text-xs font-bold flex items-center justify-center ring-4 ring-white shadow-xs">
                  {idx + 1}
                </div>

                <div className="card p-4 border border-surface-200 space-y-1.5">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded">
                      {m.timeframe || `Stage ${idx + 1}`}
                    </span>
                  </div>

                  <h4 className="font-bold text-surface-900 text-sm">
                    {m.title}
                  </h4>

                  {m.description && (
                    <p className="text-xs text-surface-600 leading-relaxed">
                      {m.description}
                    </p>
                  )}

                  {m.targetOutcome && (
                    <div className="bg-emerald-50/60 border border-emerald-100 rounded-lg p-2 text-xs text-emerald-900 mt-2">
                      <span className="font-semibold text-emerald-800">Target Outcome: </span>
                      {m.targetOutcome}
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Certifications (if any) */}
      {certifications.length > 0 && (
        <div className="space-y-4 pt-4 border-t border-surface-200">
          <div className="flex items-center gap-2">
            <Award className="h-5 w-5 text-purple-600" />
            <h3 className="section-title mb-0">Recommended Certifications</h3>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {certifications.map((cert, idx) => (
              <div key={idx} className="card p-4 border border-surface-200 space-y-2 flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[10px] font-bold uppercase tracking-wider text-purple-700 bg-purple-50 border border-purple-200 px-2 py-0.5 rounded">
                      {cert.importance || 'Recommended'}
                    </span>
                    {cert.provider && (
                      <span className="text-xs text-surface-500 font-medium">
                        {cert.provider}
                      </span>
                    )}
                  </div>

                  <h4 className="font-bold text-surface-900 text-sm mt-1">
                    {cert.name}
                  </h4>
                </div>

                {cert.url && (
                  <div className="pt-2 border-t border-surface-100">
                    <a
                      href={cert.url}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-xs text-primary-600 hover:text-primary-800 flex items-center gap-1 hover:underline font-semibold"
                    >
                      <span>Explore Certification Info</span>
                      <ExternalLink className="h-3 w-3" />
                    </a>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
