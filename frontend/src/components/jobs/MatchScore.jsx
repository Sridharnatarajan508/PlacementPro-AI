import { CheckCircle2, Award, Zap, BookOpen, Clock } from 'lucide-react'

export function getScoreTheme(score) {
  if (score >= 80) return { bg: 'bg-emerald-50 text-emerald-700 border-emerald-200', badge: 'bg-emerald-500 text-white', bar: 'bg-emerald-500', text: 'text-emerald-600', ring: 'ring-emerald-400' }
  if (score >= 65) return { bg: 'bg-blue-50 text-blue-700 border-blue-200', badge: 'bg-blue-500 text-white', bar: 'bg-blue-500', text: 'text-blue-600', ring: 'ring-blue-400' }
  if (score >= 50) return { bg: 'bg-amber-50 text-amber-700 border-amber-200', badge: 'bg-amber-500 text-white', bar: 'bg-amber-500', text: 'text-amber-600', ring: 'ring-amber-400' }
  return { bg: 'bg-rose-50 text-rose-700 border-rose-200', badge: 'bg-rose-500 text-white', bar: 'bg-rose-500', text: 'text-rose-600', ring: 'ring-rose-400' }
}

export function formatRecommendation(rec) {
  switch (rec) {
    case 'STRONG_MATCH': return 'Strong Match'
    case 'GOOD_MATCH': return 'Good Match'
    case 'PARTIAL_MATCH': return 'Partial Match'
    case 'LOW_MATCH': return 'Low Match'
    default: return rec ? rec.replace('_', ' ') : 'Match Analyzed'
  }
}

export default function MatchScore({ overallScore, skillScore, experienceScore, educationScore, recommendation, compact = false }) {
  const theme = getScoreTheme(overallScore || 0)
  const label = formatRecommendation(recommendation)

  if (compact) {
    return (
      <div className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border ${theme.bg}`}>
        <Zap className="h-3.5 w-3.5" />
        <span>{overallScore}% Match</span>
      </div>
    )
  }

  return (
    <div className="bg-white rounded-xl border border-surface-200 p-5 shadow-sm space-y-4">
      {/* Overall Score Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className={`w-14 h-14 rounded-2xl flex flex-col items-center justify-center font-bold border-2 ${theme.bg} ${theme.ring}`}>
            <span className="text-xl leading-none">{overallScore}%</span>
            <span className="text-[10px] font-medium opacity-80 mt-0.5">FIT</span>
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className={`inline-block px-2.5 py-0.5 rounded-full text-xs font-bold ${theme.badge}`}>
                {label}
              </span>
            </div>
            <p className="text-xs text-surface-500 mt-1">AI-calculated compatibility with your profile & resume</p>
          </div>
        </div>
      </div>

      {/* Breakdown bars */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-2 border-t border-surface-100">
        {/* Skill Match */}
        <div className="space-y-1">
          <div className="flex justify-between text-xs font-medium">
            <span className="flex items-center gap-1 text-surface-600">
              <Zap className="h-3.5 w-3.5 text-primary-500" />
              Skills Match
            </span>
            <span className="font-semibold text-surface-800">{skillScore ?? 0}%</span>
          </div>
          <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${getScoreTheme(skillScore || 0).bar}`}
              style={{ width: `${Math.min(100, Math.max(0, skillScore || 0))}%` }}
            />
          </div>
        </div>

        {/* Experience Match */}
        <div className="space-y-1">
          <div className="flex justify-between text-xs font-medium">
            <span className="flex items-center gap-1 text-surface-600">
              <Clock className="h-3.5 w-3.5 text-blue-500" />
              Experience Match
            </span>
            <span className="font-semibold text-surface-800">{experienceScore ?? 0}%</span>
          </div>
          <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${getScoreTheme(experienceScore || 0).bar}`}
              style={{ width: `${Math.min(100, Math.max(0, experienceScore || 0))}%` }}
            />
          </div>
        </div>

        {/* Education Match */}
        <div className="space-y-1">
          <div className="flex justify-between text-xs font-medium">
            <span className="flex items-center gap-1 text-surface-600">
              <BookOpen className="h-3.5 w-3.5 text-purple-500" />
              Education Match
            </span>
            <span className="font-semibold text-surface-800">{educationScore ?? 0}%</span>
          </div>
          <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
            <div
              className={`h-full rounded-full ${getScoreTheme(educationScore || 0).bar}`}
              style={{ width: `${Math.min(100, Math.max(0, educationScore || 0))}%` }}
            />
          </div>
        </div>
      </div>
    </div>
  )
}
