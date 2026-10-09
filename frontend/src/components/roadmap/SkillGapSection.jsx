import { Zap, Clock, ArrowRight, CheckCircle, AlertTriangle, BookOpen } from 'lucide-react'

export default function SkillGapSection({ skillsToLearn = [] }) {
  if (!skillsToLearn || skillsToLearn.length === 0) {
    return (
      <div className="card p-6 border border-surface-200 text-center">
        <CheckCircle className="h-8 w-8 text-emerald-600 mx-auto mb-2" />
        <p className="text-xs text-surface-600">No specific prioritized skill gaps listed.</p>
      </div>
    )
  }

  const getPriorityBadge = (p) => {
    switch (p?.toUpperCase()) {
      case 'HIGH': return 'bg-rose-100 text-rose-800 border-rose-200'
      case 'MEDIUM': return 'bg-amber-100 text-amber-800 border-amber-200'
      case 'LOW': return 'bg-blue-100 text-blue-800 border-blue-200'
      default: return 'bg-surface-100 text-surface-700 border-surface-200'
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <Zap className="h-5 w-5 text-amber-500" />
        <h3 className="section-title mb-0">Prioritized Skills to Master</h3>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {skillsToLearn.map((skill, idx) => (
          <div key={idx} className="card p-4 border border-surface-200 hover:shadow-sm transition flex flex-col justify-between space-y-3">
            <div>
              <div className="flex items-center justify-between gap-2 mb-1.5">
                <span className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase tracking-wider ${getPriorityBadge(skill.priority)}`}>
                  {skill.priority || 'MEDIUM'} PRIORITY
                </span>
                {skill.estimatedHours && (
                  <span className="text-xs font-semibold text-surface-500 flex items-center gap-1">
                    <Clock className="h-3 w-3 text-purple-500" />
                    {skill.estimatedHours}h
                  </span>
                )}
              </div>

              <h4 className="font-bold text-surface-900 text-sm">
                {skill.name}
              </h4>
              {skill.category && (
                <span className="text-[11px] text-surface-400 font-medium block mt-0.5">
                  Domain: {skill.category}
                </span>
              )}

              {skill.reason && (
                <p className="text-xs text-surface-600 mt-2 leading-relaxed">
                  <span className="font-semibold text-surface-700">Career Value: </span>
                  {skill.reason}
                </p>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
