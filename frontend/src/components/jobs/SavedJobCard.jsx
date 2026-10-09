import { Building2, MapPin, IndianRupee, Trash2, Sparkles, ExternalLink } from 'lucide-react'
import { getScoreTheme, formatRecommendation } from './MatchScore'

export default function SavedJobCard({ job, onUnsave, onViewDetails, onAnalyze }) {
  const formatSalary = (min, max) => {
    if (!min && !max) return null
    const toLPA = (val) => (val ? (Number(val) / 100000).toFixed(1) + ' LPA' : '')
    if (min && max) return `₹${toLPA(min)} - ₹${toLPA(max)}`
    if (min) return `From ₹${toLPA(min)}`
    return `Up to ₹${toLPA(max)}`
  }

  const salaryStr = formatSalary(job.salaryMin, job.salaryMax)
  const hasMatch = job.matchScore != null
  const scoreTheme = hasMatch ? getScoreTheme(job.matchScore) : null

  return (
    <div className="card p-4 hover:shadow-sm transition flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 border border-surface-200">
      <div className="space-y-1 min-w-0 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-surface-100 text-surface-700">
            {job.jobType ? job.jobType.replace('_', ' ') : 'Full Time'}
          </span>
          <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-primary-50 text-primary-700">
            {job.experienceLevel || 'Fresher'}
          </span>
          {hasMatch && (
            <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full border ${scoreTheme.bg}`}>
              {job.matchScore}% Match • {formatRecommendation(job.matchRecommendation)}
            </span>
          )}
        </div>
        <h3
          className="font-bold text-surface-900 text-base hover:text-primary-600 transition cursor-pointer truncate"
          onClick={() => onViewDetails(job)}
        >
          {job.title}
        </h3>
        <div className="flex flex-wrap items-center gap-x-3 text-xs text-surface-500">
          <span className="flex items-center gap-1 text-surface-700 font-medium">
            <Building2 className="h-3.5 w-3.5 text-surface-400" />
            {job.company}
          </span>
          <span className="flex items-center gap-1">
            <MapPin className="h-3.5 w-3.5 text-surface-400" />
            {job.location}
          </span>
          {salaryStr && (
            <span className="flex items-center gap-1 font-medium text-emerald-700">
              <IndianRupee className="h-3.5 w-3.5 text-emerald-600" />
              {salaryStr}
            </span>
          )}
        </div>
      </div>

      <div className="flex items-center gap-2 self-end sm:self-center flex-shrink-0">
        <button
          onClick={() => onAnalyze(job.id)}
          className={`btn btn-xs inline-flex items-center gap-1 ${
            hasMatch ? 'btn-secondary text-primary-700 border-primary-200' : 'btn-primary'
          }`}
        >
          <Sparkles className="h-3 w-3" />
          <span>{hasMatch ? 'View Match' : 'Analyze Match'}</span>
        </button>

        <button
          onClick={() => onViewDetails(job)}
          className="btn-secondary btn btn-xs"
        >
          Details
        </button>

        <button
          onClick={() => onUnsave(job.id)}
          className="p-1.5 rounded-lg text-rose-500 hover:bg-rose-50 border border-surface-200 hover:border-rose-200 transition"
          title="Remove from saved jobs"
        >
          <Trash2 className="h-4 w-4" />
        </button>
      </div>
    </div>
  )
}
