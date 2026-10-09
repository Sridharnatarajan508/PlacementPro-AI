import { Sparkles, XCircle, AlertTriangle } from 'lucide-react'

export default function InterviewProgress({
  currentNumber = 1,
  totalQuestions = 10,
  targetRole = '',
  interviewType = '',
  difficulty = '',
  onAbandon
}) {
  const percentage = Math.round(((currentNumber - 1) / totalQuestions) * 100)

  return (
    <div className="card p-4 border border-surface-200 shadow-xs space-y-3">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="badge-primary text-xs font-bold px-2.5 py-0.5 rounded-full">
            {targetRole}
          </span>
          <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-surface-100 text-surface-700 uppercase">
            {interviewType}
          </span>
          <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-amber-50 text-amber-800 border border-amber-200 uppercase">
            {difficulty}
          </span>
        </div>

        <div className="flex items-center gap-3">
          <span className="text-xs font-bold text-surface-900">
            Question {currentNumber} of {totalQuestions}
          </span>

          {onAbandon && (
            <button
              onClick={onAbandon}
              className="text-xs text-surface-400 hover:text-rose-600 flex items-center gap-1 transition"
            >
              <XCircle className="h-3.5 w-3.5" />
              <span>Exit</span>
            </button>
          )}
        </div>
      </div>

      <div className="space-y-1">
        <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-primary-600 rounded-full transition-all duration-500 ease-out"
            style={{ width: `${percentage}%` }}
          />
        </div>
      </div>
    </div>
  )
}
