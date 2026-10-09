import { HelpCircle, BookOpen, Layers, CheckCircle2, MessageSquare } from 'lucide-react'

export default function InterviewPreparation({ interviewTopics = [] }) {
  if (!interviewTopics || interviewTopics.length === 0) {
    return (
      <div className="card p-6 border border-surface-200 text-center">
        <HelpCircle className="h-8 w-8 text-surface-400 mx-auto mb-2" />
        <p className="text-xs text-surface-600">No interview topics generated.</p>
      </div>
    )
  }

  const getCategoryBadge = (cat) => {
    switch (cat?.toUpperCase()) {
      case 'DSA': return 'bg-amber-50 text-amber-800 border-amber-200'
      case 'SPRING_BOOT': return 'bg-emerald-50 text-emerald-800 border-emerald-200'
      case 'JAVA': return 'bg-blue-50 text-blue-800 border-blue-200'
      case 'SQL': return 'bg-purple-50 text-purple-800 border-purple-200'
      case 'SYSTEM_DESIGN': return 'bg-indigo-50 text-indigo-800 border-indigo-200'
      default: return 'bg-surface-100 text-surface-700 border-surface-200'
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-2">
        <MessageSquare className="h-5 w-5 text-blue-600" />
        <h3 className="section-title mb-0">Interview Preparation & Core Q&A</h3>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {interviewTopics.map((topic, idx) => (
          <div key={idx} className="card p-5 border border-surface-200 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${getCategoryBadge(topic.category)}`}>
                {topic.category || 'TECHNICAL'}
              </span>
            </div>

            <h4 className="font-bold text-surface-900 text-sm">
              {topic.topicName}
            </h4>

            {topic.keyQuestions?.length > 0 && (
              <div className="space-y-1.5 pt-2 border-t border-surface-100">
                <span className="text-[11px] font-semibold text-surface-500 block">
                  Key Interview Questions:
                </span>
                <ul className="space-y-1 text-xs text-surface-600">
                  {topic.keyQuestions.map((q, i) => (
                    <li key={i} className="flex items-start gap-1.5 bg-surface-50 p-2 rounded border border-surface-100">
                      <span className="text-primary-600 font-bold text-[11px]">•</span>
                      <span>{q}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
