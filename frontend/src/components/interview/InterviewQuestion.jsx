import { useState, useEffect } from 'react'
import { Send, Sparkles, HelpCircle, Code, MessageSquare, Lightbulb } from 'lucide-react'

export default function InterviewQuestion({
  question,
  onSubmitAnswer,
  submitting = false
}) {
  const [answer, setAnswer] = useState('')

  useEffect(() => {
    setAnswer('')
  }, [question?.id])

  if (!question) return null

  const getCategoryBadge = (cat) => {
    switch (cat?.toUpperCase()) {
      case 'TECHNICAL': return 'bg-blue-50 text-blue-800 border-blue-200'
      case 'BEHAVIORAL': return 'bg-purple-50 text-purple-800 border-purple-200'
      case 'DSA': return 'bg-amber-50 text-amber-800 border-amber-200'
      case 'SYSTEM_DESIGN': return 'bg-indigo-50 text-indigo-800 border-indigo-200'
      case 'HR': return 'bg-emerald-50 text-emerald-800 border-emerald-200'
      default: return 'bg-surface-100 text-surface-700 border-surface-200'
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!answer.trim() || submitting) return
    onSubmitAnswer(question.id, answer.trim())
  }

  const charCount = answer.trim().length

  return (
    <div className="card p-6 border border-surface-200 shadow-sm space-y-5">
      {/* Question Header */}
      <div className="flex items-center justify-between gap-2 border-b border-surface-100 pb-3">
        <div className="flex items-center gap-2">
          <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${getCategoryBadge(question.category)}`}>
            {question.category || 'TECHNICAL'}
          </span>
          <span className="text-[10px] font-semibold text-surface-500 bg-surface-100 px-2 py-0.5 rounded">
            {question.difficulty || 'MEDIUM'}
          </span>
        </div>
        <span className="text-xs font-semibold text-surface-400">
          Question #{question.questionNumber}
        </span>
      </div>

      {/* Question Prompt */}
      <div className="space-y-2">
        <h3 className="text-base font-bold text-surface-900 leading-relaxed">
          {question.questionText}
        </h3>
        <p className="text-xs text-surface-500 flex items-center gap-1">
          <Lightbulb className="h-3.5 w-3.5 text-amber-500 flex-shrink-0" />
          <span>Tip: Structure your response with concrete examples, key trade-offs, and technical rationale.</span>
        </p>
      </div>

      {/* Answer Form */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-1">
          <div className="flex justify-between items-center text-xs text-surface-500 font-medium">
            <label className="font-semibold text-surface-700">Your Response</label>
            <span>{charCount} characters</span>
          </div>

          <textarea
            rows={7}
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            disabled={submitting}
            placeholder="Type your detailed answer here as you would communicate it in an interview..."
            className="input w-full p-3.5 text-xs font-normal leading-relaxed resize-y min-h-[140px]"
            required
          />
        </div>

        <div className="flex items-center justify-between pt-2">
          <span className="text-[11px] text-surface-400">
            {submitting ? 'AI is evaluating your technical depth and articulation...' : 'Press Submit when ready for AI evaluation'}
          </span>

          <button
            type="submit"
            disabled={submitting || charCount < 5}
            className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
          >
            {submitting ? (
              <>
                <Sparkles className="h-3.5 w-3.5 animate-spin" />
                <span>Evaluating with AI...</span>
              </>
            ) : (
              <>
                <Send className="h-3.5 w-3.5" />
                <span>Submit Answer</span>
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  )
}
