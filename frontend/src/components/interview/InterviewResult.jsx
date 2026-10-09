import { useState } from 'react'
import {
  Award, CheckCircle2, AlertTriangle, Lightbulb,
  Sparkles, RotateCcw, History, ChevronDown, ChevronUp, BookOpen
} from 'lucide-react'

export default function InterviewResult({
  session,
  onRetake,
  onViewHistory
}) {
  const [expandedQuestionId, setExpandedQuestionId] = useState(null)

  if (!session) return null

  const getScoreTheme = (score) => {
    if (score >= 80) return 'text-emerald-700 bg-emerald-50 border-emerald-300'
    if (score >= 60) return 'text-amber-700 bg-amber-50 border-amber-300'
    return 'text-rose-700 bg-rose-50 border-rose-300'
  }

  const toggleQuestion = (id) => {
    setExpandedQuestionId(expandedQuestionId === id ? null : id)
  }

  return (
    <div className="space-y-6 animate-slide-up max-w-4xl mx-auto">
      {/* Header Banner */}
      <div className="card p-6 border border-primary-200 bg-gradient-to-r from-primary-50/50 via-white to-primary-50/30 flex flex-col sm:flex-row items-center justify-between gap-6 shadow-sm">
        <div className="space-y-1 text-center sm:text-left">
          <div className="flex items-center justify-center sm:justify-start gap-2">
            <Award className="h-6 w-6 text-primary-600" />
            <h2 className="text-xl font-bold text-surface-900">Mock Interview Complete!</h2>
          </div>
          <p className="text-xs text-surface-600">
            Target: <span className="font-semibold text-surface-900">{session.targetRole}</span> • {session.totalQuestions} Questions Evaluated ({session.difficulty} Level)
          </p>
        </div>

        {/* Overall Score Badge */}
        <div className={`px-6 py-3 rounded-2xl border-2 font-bold text-center flex flex-col items-center justify-center shadow-xs ${getScoreTheme(session.overallScore || 0)}`}>
          <span className="text-3xl leading-none">{session.overallScore || 0}%</span>
          <span className="text-[10px] uppercase tracking-wider opacity-80 mt-1">OVERALL READINESS</span>
        </div>
      </div>

      {/* 5 Core Dimension Scores */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        {[
          { label: 'Technical Depth', score: session.technicalScore, color: 'text-blue-600' },
          { label: 'Communication', score: session.communicationScore, color: 'text-emerald-600' },
          { label: 'Confidence', score: session.confidenceScore, color: 'text-amber-600' },
          { label: 'Problem Solving', score: session.problemSolvingScore, color: 'text-purple-600' },
        ].map(({ label, score, color }) => (
          <div key={label} className="card p-4 border border-surface-200 text-center space-y-2">
            <span className="text-xs font-semibold text-surface-500 block truncate">{label}</span>
            <span className={`text-2xl font-bold ${color}`}>{score || 0}%</span>
            <div className="h-1.5 w-full bg-surface-100 rounded-full overflow-hidden">
              <div className="h-full bg-primary-600 rounded-full" style={{ width: `${score || 0}%` }} />
            </div>
          </div>
        ))}
      </div>

      {/* AI Executive Summary */}
      {session.aiSummary && (
        <div className="card p-5 border border-primary-200 bg-primary-50/20 space-y-2 shadow-xs">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 text-primary-600" />
            <h3 className="text-sm font-bold text-surface-900">AI Hiring Committee Summary</h3>
          </div>
          <p className="text-xs text-surface-700 leading-relaxed">
            {session.aiSummary}
          </p>
        </div>
      )}

      {/* Strengths & Weaknesses */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Strengths */}
        <div className="card p-5 border border-surface-200 space-y-3">
          <div className="flex items-center gap-2">
            <CheckCircle2 className="h-4 w-4 text-emerald-600" />
            <h4 className="text-xs font-bold text-surface-900">Key Competencies & Strengths</h4>
          </div>
          <ul className="space-y-1.5 text-xs text-surface-600">
            {session.strengths?.map((s, i) => (
              <li key={i} className="flex items-start gap-2 bg-surface-50 p-2.5 rounded-lg border border-surface-100">
                <span className="text-emerald-600 font-bold">✓</span>
                <span>{s}</span>
              </li>
            ))}
          </ul>
        </div>

        {/* Weaknesses */}
        <div className="card p-5 border border-surface-200 space-y-3">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-amber-600" />
            <h4 className="text-xs font-bold text-surface-900">Identified Gaps to Address</h4>
          </div>
          <ul className="space-y-1.5 text-xs text-surface-600">
            {session.weaknesses?.map((w, i) => (
              <li key={i} className="flex items-start gap-2 bg-surface-50 p-2.5 rounded-lg border border-surface-100">
                <span className="text-amber-600 font-bold">!</span>
                <span>{w}</span>
              </li>
            ))}
          </ul>
        </div>
      </div>

      {/* Recommended Topics */}
      {session.recommendations?.length > 0 && (
        <div className="card p-5 border border-surface-200 space-y-3">
          <div className="flex items-center gap-2">
            <Lightbulb className="h-4 w-4 text-amber-500" />
            <h4 className="text-xs font-bold text-surface-900">Actionable Study & Interview Prep Topics</h4>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
            {session.recommendations.map((rec, i) => (
              <div key={i} className="bg-amber-50/50 border border-amber-100 p-3 rounded-lg text-xs text-amber-900 flex items-start gap-2">
                <span className="font-bold text-amber-700">#{i + 1}</span>
                <span>{rec}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Full Q&A Review Breakdown */}
      {session.questions?.length > 0 && (
        <div className="space-y-3 pt-2">
          <h3 className="section-title mb-0">Detailed Q&A Evaluation Breakdown</h3>

          <div className="space-y-3">
            {session.questions.map((q) => {
              const isExpanded = expandedQuestionId === q.id
              return (
                <div key={q.id} className="card border border-surface-200 overflow-hidden shadow-xs">
                  <button
                    onClick={() => toggleQuestion(q.id)}
                    className="w-full p-4 text-left flex items-center justify-between gap-3 hover:bg-surface-50 transition"
                  >
                    <div className="space-y-1">
                      <div className="flex items-center gap-2">
                        <span className="text-[10px] font-bold text-surface-500 bg-surface-100 px-2 py-0.5 rounded">
                          Q#{q.questionNumber}
                        </span>
                        <span className="text-[10px] font-bold px-2 py-0.5 rounded uppercase bg-blue-50 text-blue-700 border border-blue-200">
                          {q.category}
                        </span>
                        <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                          Score: {q.score || 0}/100
                        </span>
                      </div>
                      <p className="text-xs font-semibold text-surface-900 line-clamp-1">
                        {q.questionText}
                      </p>
                    </div>

                    {isExpanded ? <ChevronUp className="h-4 w-4 text-surface-400 flex-shrink-0" /> : <ChevronDown className="h-4 w-4 text-surface-400 flex-shrink-0" />}
                  </button>

                  {isExpanded && (
                    <div className="p-4 bg-surface-50/50 border-t border-surface-200 space-y-4 text-xs">
                      <div>
                        <span className="font-bold text-surface-500 block mb-1">Your Submitted Answer:</span>
                        <p className="bg-white p-3 rounded-lg border border-surface-200 text-surface-800 leading-relaxed font-sans">
                          {q.userAnswer || '(No answer recorded)'}
                        </p>
                      </div>

                      <div>
                        <span className="font-bold text-surface-500 block mb-1">AI Evaluator Feedback:</span>
                        <p className="bg-primary-50/40 p-3 rounded-lg border border-primary-100 text-surface-800 leading-relaxed">
                          {q.aiFeedback}
                        </p>
                      </div>

                      {q.idealAnswer && (
                        <div>
                          <span className="font-bold text-surface-500 block mb-1">Ideal Model Answer:</span>
                          <p className="bg-surface-100 p-3 rounded-lg border border-surface-200 text-surface-700 leading-relaxed font-mono">
                            {q.idealAnswer}
                          </p>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
        </div>
      )}

      {/* Action Footer */}
      <div className="flex flex-wrap items-center justify-between gap-3 pt-4 border-t border-surface-200">
        <button
          onClick={onViewHistory}
          className="btn-secondary btn btn-sm inline-flex items-center gap-1.5"
        >
          <History className="h-3.5 w-3.5" />
          <span>Interview History</span>
        </button>

        <div className="flex items-center gap-2">
          <a
            href={`/ai-assistant?prompt=${encodeURIComponent(`I just completed a mock interview for ${session.targetRole} with an overall score of ${session.overallScore || 0}%. Can you analyze my performance, explain my weak areas, and suggest targeted improvements?`)}`}
            className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-xs"
          >
            <Sparkles className="h-3.5 w-3.5" />
            <span>Discuss Performance with AI</span>
          </a>

          <button
            onClick={onRetake}
            className="btn-secondary btn btn-sm inline-flex items-center gap-1.5"
          >
            <RotateCcw className="h-3.5 w-3.5" />
            <span>Practice Again</span>
          </button>
        </div>
      </div>
    </div>
  )
}
