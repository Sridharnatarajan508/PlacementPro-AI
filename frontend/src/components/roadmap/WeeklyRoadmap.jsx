import { useState } from 'react'
import {
  CheckSquare, Square, Clock, ExternalLink, ChevronDown, ChevronUp,
  Tag, Code, BookOpen, Layers, Award, Sparkles, CheckCircle2
} from 'lucide-react'

export default function WeeklyRoadmap({ tasks = [], weeklyPlan = [], onToggleTask, updatingTaskId }) {
  // Group tasks by weekNumber
  const tasksByWeek = tasks.reduce((acc, task) => {
    const w = task.weekNumber || 1
    if (!acc[w]) acc[w] = []
    acc[w].push(task)
    return acc
  }, {})

  // Weeks list from weeklyPlan or from task keys
  const weeks = weeklyPlan.length > 0
    ? weeklyPlan
    : Object.keys(tasksByWeek).sort((a, b) => Number(a) - Number(b)).map(w => ({
        weekNumber: Number(w),
        theme: `Week ${w} Curriculum`,
        goals: 'Complete weekly tasks and milestone exercises.'
      }))

  const [expandedWeeks, setExpandedWeeks] = useState(() => {
    // Expand the first 2 weeks by default
    return weeks.reduce((acc, w, idx) => {
      acc[w.weekNumber] = idx < 3
      return acc
    }, {})
  })

  const toggleWeek = (wNum) => {
    setExpandedWeeks(prev => ({
      ...prev,
      [wNum]: !prev[wNum]
    }))
  }

  const getCategoryTheme = (cat) => {
    switch (cat?.toUpperCase()) {
      case 'PROJECT': return 'bg-purple-50 text-purple-700 border-purple-200'
      case 'INTERVIEW': return 'bg-rose-50 text-rose-700 border-rose-200'
      case 'DSA': return 'bg-amber-50 text-amber-700 border-amber-200'
      case 'SYSTEM_DESIGN': return 'bg-indigo-50 text-indigo-700 border-indigo-200'
      case 'CERTIFICATION': return 'bg-emerald-50 text-emerald-700 border-emerald-200'
      case 'SOFT_SKILL': return 'bg-cyan-50 text-cyan-700 border-cyan-200'
      default: return 'bg-blue-50 text-blue-700 border-blue-200'
    }
  }

  const getPriorityTheme = (p) => {
    switch (p?.toUpperCase()) {
      case 'HIGH': return 'text-rose-600 bg-rose-50 border-rose-200'
      case 'MEDIUM': return 'text-amber-600 bg-amber-50 border-amber-200'
      case 'LOW': return 'text-blue-600 bg-blue-50 border-blue-200'
      default: return 'text-surface-600 bg-surface-50 border-surface-200'
    }
  }

  return (
    <div className="space-y-4">
      {weeks.map((week) => {
        const weekTasks = tasksByWeek[week.weekNumber] || []
        const completedCount = weekTasks.filter(t => t.completed).count || weekTasks.filter(t => t.completed).length
        const isWeekComplete = weekTasks.length > 0 && completedCount === weekTasks.length
        const isExpanded = !!expandedWeeks[week.weekNumber]

        const weekHours = weekTasks.reduce((sum, t) => sum + (t.estimatedHours || 0), 0)

        return (
          <div
            key={week.weekNumber}
            className={`card border transition-all ${
              isWeekComplete
                ? 'border-emerald-200 bg-emerald-50/10'
                : 'border-surface-200 bg-white'
            }`}
          >
            {/* Week Header Accordion */}
            <button
              onClick={() => toggleWeek(week.weekNumber)}
              className="w-full p-4 flex items-center justify-between text-left hover:bg-surface-50/50 rounded-xl transition"
            >
              <div className="flex items-center gap-3">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-xs ${
                  isWeekComplete
                    ? 'bg-emerald-600 text-white'
                    : 'bg-primary-50 text-primary-700 border border-primary-200'
                }`}>
                  {isWeekComplete ? <CheckCircle2 className="h-4 w-4" /> : `W${week.weekNumber}`}
                </div>

                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-surface-900 text-sm">
                      {week.theme || `Week ${week.weekNumber}`}
                    </h3>
                    {isWeekComplete && (
                      <span className="text-[10px] font-bold uppercase tracking-wider text-emerald-700 bg-emerald-100 px-2 py-0.5 rounded-full">
                        Completed
                      </span>
                    )}
                  </div>
                  {week.goals && (
                    <p className="text-xs text-surface-500 mt-0.5 line-clamp-1">
                      {week.goals}
                    </p>
                  )}
                </div>
              </div>

              <div className="flex items-center gap-4">
                <div className="text-right hidden sm:block">
                  <span className="text-xs font-semibold text-surface-700">
                    {completedCount} / {weekTasks.length} tasks
                  </span>
                  <span className="text-[11px] text-surface-400 block">
                    ~{weekHours} hrs
                  </span>
                </div>

                <div className="p-1 rounded-md text-surface-400 hover:text-surface-600">
                  {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </div>
              </div>
            </button>

            {/* Expanded Week Tasks */}
            {isExpanded && (
              <div className="p-4 pt-0 space-y-2.5 border-t border-surface-100 mt-2">
                {weekTasks.length === 0 ? (
                  <p className="text-xs text-surface-400 italic py-2">No tasks logged for this week.</p>
                ) : (
                  weekTasks.map((task) => (
                    <div
                      key={task.id}
                      className={`p-3.5 rounded-xl border transition flex items-start justify-between gap-3 ${
                        task.completed
                          ? 'bg-surface-50/80 border-surface-200 opacity-75'
                          : 'bg-white border-surface-200 hover:border-primary-200 hover:shadow-2xs'
                      }`}
                    >
                      {/* Checkbox & Task info */}
                      <div className="flex items-start gap-3 flex-1">
                        <button
                          onClick={() => onToggleTask(task)}
                          disabled={updatingTaskId === task.id}
                          className="mt-0.5 text-surface-400 hover:text-primary-600 transition flex-shrink-0"
                          title={task.completed ? 'Mark as incomplete' : 'Mark as completed'}
                        >
                          {task.completed ? (
                            <CheckSquare className="h-5 w-5 text-emerald-600" />
                          ) : (
                            <Square className="h-5 w-5 hover:text-primary-600" />
                          )}
                        </button>

                        <div className="space-y-1 flex-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded border ${getCategoryTheme(task.category)}`}>
                              {task.category || 'SKILL'}
                            </span>
                            {task.priority && (
                              <span className={`text-[10px] font-semibold px-1.5 py-0.5 rounded border ${getPriorityTheme(task.priority)}`}>
                                {task.priority}
                              </span>
                            )}
                            {task.estimatedHours && (
                              <span className="text-[11px] text-surface-400 flex items-center gap-1">
                                <Clock className="h-3 w-3" />
                                {task.estimatedHours}h
                              </span>
                            )}
                          </div>

                          <h4 className={`text-xs font-bold text-surface-900 ${task.completed ? 'line-through text-surface-500' : ''}`}>
                            {task.title}
                          </h4>

                          {task.description && (
                            <p className="text-xs text-surface-600 leading-relaxed">
                              {task.description}
                            </p>
                          )}
                        </div>
                      </div>

                      {/* Resource link */}
                      {task.resourceUrl && (
                        <a
                          href={task.resourceUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-xs text-primary-600 hover:text-primary-800 flex items-center gap-1 hover:underline flex-shrink-0 self-center"
                          title="Open learning resource"
                        >
                          <ExternalLink className="h-3.5 w-3.5" />
                        </a>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
