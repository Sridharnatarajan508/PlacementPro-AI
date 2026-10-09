import { Sparkles, Target, Award, Clock, Calendar, RefreshCw } from 'lucide-react'

export default function RoadmapHeader({ roadmap, onRegenerate, regenerating }) {
  if (!roadmap) return null

  const {
    targetRole,
    currentLevel,
    overallProgress = 0,
    estimatedDurationWeeks,
    aiSummary,
    createdAt
  } = roadmap

  return (
    <div className="card p-6 border border-surface-200 shadow-sm space-y-4">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 flex-wrap mb-1">
            <span className="badge-primary text-xs font-semibold px-2.5 py-0.5 rounded-full flex items-center gap-1">
              <Target className="h-3 w-3" />
              {targetRole}
            </span>
            <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-surface-100 text-surface-700">
              Level: {currentLevel || 'Fresher / Entry Level'}
            </span>
            <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-purple-50 text-purple-700 flex items-center gap-1">
              <Clock className="h-3 w-3" />
              {estimatedDurationWeeks || 12} Weeks
            </span>
          </div>

          <h1 className="text-2xl font-bold text-surface-900">
            Career Mastery Roadmap
          </h1>
          <p className="text-xs text-surface-400 mt-0.5 flex items-center gap-1.5">
            <Calendar className="h-3.5 w-3.5" />
            Generated on {new Date(createdAt).toLocaleDateString()}
          </p>
        </div>

        {/* Action button */}
        <div className="flex items-center gap-3">
          <button
            onClick={onRegenerate}
            disabled={regenerating}
            className="btn-secondary btn btn-sm inline-flex items-center gap-1.5"
          >
            <RefreshCw className={`h-3.5 w-3.5 ${regenerating ? 'animate-spin' : ''}`} />
            <span>{regenerating ? 'Regenerating...' : 'Regenerate Plan'}</span>
          </button>
        </div>
      </div>

      {/* AI Summary */}
      {aiSummary && (
        <div className="bg-primary-50/70 border border-primary-100 rounded-xl p-4 text-xs text-primary-950 leading-relaxed shadow-2xs">
          <span className="font-bold uppercase tracking-wider block text-primary-900 text-[10px] mb-1 flex items-center gap-1">
            <Sparkles className="h-3.5 w-3.5 text-primary-600" />
            Strategic Learning Objective
          </span>
          {aiSummary}
        </div>
      )}
    </div>
  )
}
