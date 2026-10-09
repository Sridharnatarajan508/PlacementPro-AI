import { useState } from 'react'
import {
  CheckCircle2, AlertTriangle, ArrowRight, Sparkles,
  ChevronDown, ChevronUp, BookOpen, Award, Zap
} from 'lucide-react'

export default function AnswerFeedback({
  evaluation,
  onNext,
  isComplete = false
}) {
  const [showIdealAnswer, setShowIdealAnswer] = useState(false)

  if (!evaluation) return null

  const getScoreTheme = (score) => {
    if (score >= 80) return 'text-emerald-700 bg-emerald-50 border-emerald-300'
    if (score >= 60) return 'text-amber-700 bg-amber-50 border-amber-300'
    return 'text-rose-700 bg-rose-50 border-rose-300'
  }

  return (
    <div className="card p-6 border border-surface-200 shadow-sm space-y-6 animate-slide-up">
      {/* Score Header & Metrics */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-surface-100 pb-5">
        <div>
          <span className="text-[10px] font-bold uppercase tracking-wider text-surface-400 block">
            Question #{evaluation.questionNumber} Evaluation
          </span>
          <h3 className="text-lg font-bold text-surface-900 mt-0.5">
            Answer Performance Review
          </h3>
        </div>

        <div className="flex items-center gap-3">
          <div className={`px-4 py-2 rounded-xl border font-bold text-center flex flex-col items-center justify-center ${getScoreTheme(evaluation.score)}`}>
            <span className="text-2xl leading-none">{evaluation.score}</span>
            <span className="text-[9px] uppercase tracking-wider opacity-80 mt-0.5">SCORE / 100</span>
          </div>
        </div>
      </div>

      {/* 4 Dimension Metrics */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {[
          { label: 'Technical Depth', score: evaluation.technicalScore, color: 'text-blue-600' },
          { label: 'Relevance', score: evaluation.relevanceScore, color: 'text-purple-600' },
          { label: 'Communication', score: evaluation.communicationScore, color: 'text-emerald-600' },
          { label: 'Confidence', score: evaluation.confidenceScore, color: 'text-amber-600' },
        ].map(({ label, score, color }) => (
          <div key={label} className="bg-surface-50 p-3 rounded-lg border border-surface-100 space-y-1">
            <span className="text-[11px] font-medium text-surface-500 block truncate">{label}</span>
            <div className="flex items-baseline justify-between">
              <span className={`text-base font-bold ${color}`}>{score || 0}%</span>
            </div>
            <div className="h-1.5 w-full bg-surface-200 rounded-full overflow-hidden">
              <div className={`h-full bg-primary-600 rounded-full`} style={{ width: `${score || 0}%` }} />
            </div>
          </div>
        ))}
      </div>

      {/* AI Qualitative Feedback */}
      {evaluation.feedback && (
        <div className="bg-primary-50/30 border border-primary-100 rounded-xl p-4 space-y-1">
          <span className="text-xs font-bold text-primary-900 flex items-center gap-1.5">
            <Sparkles className="h-3.5 w-3.5 text-primary-600" />
            AI Evaluator Feedback
          </span>
          <p className="text-xs text-surface-700 leading-relaxed">
            {evaluation.feedback}
          </p>
        </div>
      )}

      {/* Strengths & Improvements */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Strengths */}
        {evaluation.strengths?.length > 0 && (
          <div className="space-y-2">
            <span className="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
              <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600" />
              Demonstrated Strengths
            </span>
            <ul className="space-y-1 text-xs text-surface-600">
              {evaluation.strengths.map((s, i) => (
                <li key={i} className="flex items-start gap-1.5 bg-surface-50 p-2.5 rounded-lg border border-surface-100">
                  <span className="text-emerald-600 font-bold text-xs">✓</span>
                  <span>{s}</span>
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Improvements */}
        {evaluation.improvements?.length > 0 && (
          <div className="space-y-2">
            <span className="text-xs font-bold text-amber-800 flex items-center gap-1.5">
              <AlertTriangle className="h-3.5 w-3.5 text-amber-600" />
              Areas for Improvement
            </span>
            <ul className="space-y-1 text-xs text-surface-600">
              {evaluation.improvements.map((imp, i) => (
                <li key={i} className="flex items-start gap-1.5 bg-surface-50 p-2.5 rounded-lg border border-surface-100">
                  <span className="text-amber-600 font-bold text-xs">→</span>
                  <span>{imp}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      {/* Ideal Model Answer Accordion */}
      {evaluation.idealAnswer && (
        <div className="border border-surface-200 rounded-xl overflow-hidden">
          <button
            onClick={() => setShowIdealAnswer(!showIdealAnswer)}
            className="w-full p-3.5 bg-surface-50 hover:bg-surface-100 flex items-center justify-between text-xs font-bold text-surface-800 transition"
          >
            <span className="flex items-center gap-1.5 text-surface-700">
              <BookOpen className="h-3.5 w-3.5 text-primary-600" />
              Compare With Ideal Model Answer
            </span>
            {showIdealAnswer ? <ChevronUp className="h-4 w-4 text-surface-400" /> : <ChevronDown className="h-4 w-4 text-surface-400" />}
          </button>

          {showIdealAnswer && (
            <div className="p-4 bg-white border-t border-surface-200 text-xs text-surface-700 leading-relaxed font-mono">
              {evaluation.idealAnswer}
            </div>
          )}
        </div>
      )}

      {/* Next Action Button */}
      <div className="pt-2 border-t border-surface-100 flex justify-end">
        <button
          onClick={onNext}
          className="btn-primary btn btn-sm inline-flex items-center gap-2"
        >
          <span>{isComplete ? 'View Final Interview Evaluation' : 'Next Question'}</span>
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  )
}
