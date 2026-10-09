import {
  History, Award, Clock, ArrowRight, CheckCircle,
  XCircle, Play, Sparkles
} from 'lucide-react'

export default function InterviewHistory({
  history = [],
  onSelectSession,
  onNewInterview,
  loading = false
}) {
  const getStatusBadge = (status) => {
    switch (status?.toUpperCase()) {
      case 'COMPLETED': return 'bg-emerald-50 text-emerald-700 border-emerald-200'
      case 'IN_PROGRESS': return 'bg-amber-50 text-amber-700 border-amber-200'
      default: return 'bg-rose-50 text-rose-700 border-rose-200'
    }
  }

  const getScoreTheme = (score) => {
    if (!score && score !== 0) return 'text-surface-400'
    if (score >= 80) return 'text-emerald-600 font-bold'
    if (score >= 60) return 'text-amber-600 font-bold'
    return 'text-rose-600 font-bold'
  }

  return (
    <div className="space-y-5 max-w-4xl mx-auto animate-slide-up">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-bold text-surface-900 flex items-center gap-2">
            <History className="h-5 w-5 text-primary-600" />
            Mock Interview History
          </h2>
          <p className="text-xs text-surface-500 mt-0.5">
            Review your past mock interview evaluations, scores, and progress over time.
          </p>
        </div>

        <button
          onClick={onNewInterview}
          className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
        >
          <Play className="h-3.5 w-3.5" />
          <span>New Interview</span>
        </button>
      </div>

      {history.length === 0 ? (
        <div className="card p-8 border border-surface-200 text-center space-y-3">
          <History className="h-8 w-8 text-surface-400 mx-auto" />
          <h4 className="text-sm font-bold text-surface-900">No Interview History Yet</h4>
          <p className="text-xs text-surface-500 max-w-sm mx-auto">
            Take your first AI mock interview to receive real-time scoring, tailored technical questions, and personalized feedback.
          </p>
          <button
            onClick={onNewInterview}
            className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
          >
            <Sparkles className="h-3.5 w-3.5" />
            <span>Start Your First Interview</span>
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {history.map((item) => (
            <div
              key={item.id}
              className="card p-4 border border-surface-200 hover:shadow-sm transition flex flex-col sm:flex-row sm:items-center justify-between gap-4"
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-bold text-surface-900 text-sm">
                    {item.targetRole}
                  </span>
                  {item.targetJobTitle && (
                    <span className="text-[11px] text-surface-500 font-medium">
                      ({item.targetJobTitle})
                    </span>
                  )}
                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded border uppercase ${getStatusBadge(item.status)}`}>
                    {item.status}
                  </span>
                </div>

                <div className="flex items-center gap-3 text-xs text-surface-400">
                  <span>Type: <strong className="text-surface-700">{item.interviewType}</strong></span>
                  <span>•</span>
                  <span>Difficulty: <strong className="text-surface-700">{item.difficulty}</strong></span>
                  <span>•</span>
                  <span>{item.completedQuestions}/{item.totalQuestions} Questions</span>
                  {item.createdAt && (
                    <>
                      <span>•</span>
                      <span>{new Date(item.createdAt).toLocaleDateString()}</span>
                    </>
                  )}
                </div>
              </div>

              <div className="flex items-center justify-between sm:justify-end gap-4 pt-2 sm:pt-0 border-t sm:border-t-0 border-surface-100">
                <div className="text-right">
                  <span className="text-[10px] uppercase font-semibold text-surface-400 block">Overall Score</span>
                  <span className={`text-lg leading-none ${getScoreTheme(item.overallScore)}`}>
                    {item.overallScore !== null ? `${item.overallScore}%` : 'Incomplete'}
                  </span>
                </div>

                <button
                  onClick={() => onSelectSession(item.id)}
                  className="btn-secondary btn btn-xs inline-flex items-center gap-1"
                >
                  <span>View Details</span>
                  <ArrowRight className="h-3 w-3" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
