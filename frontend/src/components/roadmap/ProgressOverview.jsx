import { CheckCircle2, Circle, Clock, TrendingUp, Award } from 'lucide-react'

export default function ProgressOverview({ progress }) {
  if (!progress) return null

  const {
    totalTasks = 0,
    completedTasks = 0,
    progressPercentage = 0,
    completedHours = 0,
    remainingHours = 0
  } = progress

  const remainingTasks = Math.max(0, totalTasks - completedTasks)
  const totalHours = completedHours + remainingHours

  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {/* Overall Progress */}
      <div className="card p-4 border border-surface-200 shadow-sm flex flex-col justify-between space-y-2">
        <div className="flex items-center justify-between text-xs text-surface-500 font-medium">
          <span>Overall Roadmap Progress</span>
          <TrendingUp className="h-4 w-4 text-primary-600" />
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-surface-900">{progressPercentage}%</span>
          <span className="text-xs text-surface-400">completed</span>
        </div>
        <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
          <div
            className="h-full bg-primary-600 rounded-full transition-all duration-300"
            style={{ width: `${progressPercentage}%` }}
          />
        </div>
      </div>

      {/* Completed Tasks */}
      <div className="card p-4 border border-surface-200 shadow-sm flex flex-col justify-between space-y-2">
        <div className="flex items-center justify-between text-xs text-surface-500 font-medium">
          <span>Tasks Accomplished</span>
          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-surface-900">{completedTasks}</span>
          <span className="text-xs text-surface-400">/ {totalTasks} tasks</span>
        </div>
        <p className="text-[11px] text-emerald-600 font-medium">
          {remainingTasks > 0 ? `${remainingTasks} tasks remaining` : 'All tasks completed!'}
        </p>
      </div>

      {/* Completed Hours */}
      <div className="card p-4 border border-surface-200 shadow-sm flex flex-col justify-between space-y-2">
        <div className="flex items-center justify-between text-xs text-surface-500 font-medium">
          <span>Time Invested</span>
          <Clock className="h-4 w-4 text-purple-600" />
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-surface-900">{completedHours}</span>
          <span className="text-xs text-surface-400">hours completed</span>
        </div>
        <p className="text-[11px] text-surface-400">
          Estimated {totalHours} total study hours
        </p>
      </div>

      {/* Remaining Hours */}
      <div className="card p-4 border border-surface-200 shadow-sm flex flex-col justify-between space-y-2">
        <div className="flex items-center justify-between text-xs text-surface-500 font-medium">
          <span>Remaining Effort</span>
          <Award className="h-4 w-4 text-amber-600" />
        </div>
        <div className="flex items-baseline gap-2">
          <span className="text-2xl font-bold text-surface-900">~{remainingHours}</span>
          <span className="text-xs text-surface-400">hours to go</span>
        </div>
        <p className="text-[11px] text-surface-400">
          Focus on high-priority milestones
        </p>
      </div>
    </div>
  )
}
