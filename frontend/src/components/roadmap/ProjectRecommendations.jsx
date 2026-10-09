import { Code, Clock, Layers, Sparkles, CheckCircle2 } from 'lucide-react'

export default function ProjectRecommendations({ projects = [] }) {
  if (!projects || projects.length === 0) {
    return (
      <div className="card p-6 border border-surface-200 text-center">
        <Code className="h-8 w-8 text-surface-400 mx-auto mb-2" />
        <p className="text-xs text-surface-600">No project recommendations available.</p>
      </div>
    )
  }

  const getDifficultyTheme = (diff) => {
    switch (diff?.toUpperCase()) {
      case 'ADVANCED': return 'bg-rose-50 text-rose-700 border-rose-200'
      case 'INTERMEDIATE': return 'bg-amber-50 text-amber-700 border-amber-200'
      default: return 'bg-emerald-50 text-emerald-700 border-emerald-200'
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Code className="h-5 w-5 text-purple-600" />
        <h3 className="section-title mb-0">Hands-on Capstone Projects</h3>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {projects.map((proj, idx) => (
          <div key={idx} className="card p-5 border border-surface-200 hover:shadow-sm transition flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between gap-2 mb-2">
                <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${getDifficultyTheme(proj.difficulty)}`}>
                  {proj.difficulty || 'Intermediate'}
                </span>
                {proj.estimatedHours && (
                  <span className="text-xs font-semibold text-surface-500 flex items-center gap-1">
                    <Clock className="h-3 w-3 text-purple-500" />
                    ~{proj.estimatedHours}h
                  </span>
                )}
              </div>

              <h4 className="font-bold text-surface-900 text-sm">
                {proj.title}
              </h4>
              <p className="text-xs text-surface-600 mt-1 leading-relaxed">
                {proj.description}
              </p>
            </div>

            {proj.skillsDemonstrated?.length > 0 && (
              <div className="pt-2 border-t border-surface-100">
                <span className="text-[11px] font-semibold text-surface-500 block mb-1">
                  Skills Demonstrated:
                </span>
                <div className="flex flex-wrap gap-1">
                  {proj.skillsDemonstrated.map((s, i) => (
                    <span key={i} className="badge-surface text-[10px] px-2 py-0.5 rounded font-medium">
                      {s}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
