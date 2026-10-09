import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { Building2, MapPin, Briefcase, IndianRupee, Bookmark, BookmarkCheck, Sparkles, ArrowRight, BookOpen, ClipboardList, Check } from 'lucide-react'
import { toast } from 'react-hot-toast'
import { getScoreTheme, formatRecommendation } from './MatchScore'
import { jobApplicationService } from '../../services/api'
import { getErrorMessage } from '../../services/apiClient'

export default function JobCard({ job, onSaveToggle, onViewDetails, onAnalyze, analyzingId }) {
  const isAnalyzing = analyzingId === job.id
  const [isTracked, setIsTracked] = useState(false)
  const [tracking, setTracking] = useState(false)

  const formatSalary = (min, max) => {
    if (!min && !max) return null
    const toLPA = (val) => (val ? (Number(val) / 100000).toFixed(1) + ' LPA' : '')
    if (min && max) return `₹${toLPA(min)} - ₹${toLPA(max)}`
    if (min) return `From ₹${toLPA(min)}`
    return `Up to ₹${toLPA(max)}`
  }

  const salaryStr = formatSalary(job.salaryMin, job.salaryMax)
  const skillsList = job.requiredSkills ? job.requiredSkills.split(',').map(s => s.trim()).filter(Boolean) : []
  const hasMatch = job.matchScore != null

  const scoreTheme = hasMatch ? getScoreTheme(job.matchScore) : null

  const handleTrackApplication = async () => {
    if (tracking) return
    setTracking(true)
    try {
      await jobApplicationService.createApplication(job.id)
      setIsTracked(true)
      toast.success('Job added to your Application Tracker pipeline!')
    } catch (err) {
      const msg = getErrorMessage(err)
      if (msg && msg.toLowerCase().includes('already')) {
        setIsTracked(true)
        toast.info('Job is already tracked in your applications.')
      } else {
        toast.error(msg || 'Could not track application.')
      }
    } finally {
      setTracking(false)
    }
  }

  return (
    <div className="card p-5 hover:shadow-md transition duration-200 flex flex-col justify-between space-y-4 border border-surface-200">
      {/* Top Header */}
      <div>
        <div className="flex items-start justify-between gap-3">
          <div className="space-y-1 min-w-0">
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-surface-100 text-surface-700">
                {job.jobType ? job.jobType.replace('_', ' ') : 'Full Time'}
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded bg-primary-50 text-primary-700">
                {job.experienceLevel || 'Fresher'}
              </span>
              {hasMatch && (
                <span className={`text-xs font-bold px-2 py-0.5 rounded-full border ${scoreTheme.bg}`}>
                  {job.matchScore}% Match • {formatRecommendation(job.matchRecommendation)}
                </span>
              )}
            </div>
            <h3 className="font-bold text-surface-900 text-base leading-snug hover:text-primary-600 transition cursor-pointer"
                onClick={() => onViewDetails(job)}>
              {job.title}
            </h3>
            <div className="flex items-center gap-2 text-xs font-medium text-surface-600">
              <Building2 className="h-3.5 w-3.5 text-surface-400" />
              <span>{job.company}</span>
            </div>
          </div>

          {/* Bookmark Button */}
          <button
            onClick={() => onSaveToggle(job.id, job.isSaved)}
            className={`p-2 rounded-lg transition ${
              job.isSaved
                ? 'text-primary-600 bg-primary-50 hover:bg-primary-100'
                : 'text-surface-400 hover:text-surface-600 hover:bg-surface-100'
            }`}
            title={job.isSaved ? 'Remove from saved' : 'Save job'}
          >
            {job.isSaved ? (
              <BookmarkCheck className="h-5 w-5 fill-primary-600" />
            ) : (
              <Bookmark className="h-5 w-5" />
            )}
          </button>
        </div>

        {/* Meta Info */}
        <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-surface-500 mt-3 pt-3 border-t border-surface-100">
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

        {/* Skills preview */}
        <div className="flex flex-wrap gap-1.5 mt-3">
          {skillsList.slice(0, 4).map((skill, idx) => (
            <span key={idx} className="bg-surface-50 border border-surface-200 text-surface-700 text-[11px] font-medium px-2 py-0.5 rounded">
              {skill}
            </span>
          ))}
          {skillsList.length > 4 && (
            <span className="text-[11px] font-medium text-surface-400 self-center">
              +{skillsList.length - 4} more
            </span>
          )}
        </div>
      </div>

      {/* Bottom CTA Actions */}
      <div className="flex items-center justify-between gap-2 pt-3 border-t border-surface-100 flex-wrap">
        <div className="flex items-center gap-1.5 flex-wrap">
          <button
            onClick={() => onViewDetails(job)}
            className="btn-secondary btn btn-xs"
          >
            Details
          </button>
          
          <Link
            to={`/preparation?jobId=${job.id}`}
            className="btn-secondary btn btn-xs text-primary-700 border-primary-200 hover:bg-primary-50 inline-flex items-center gap-1"
          >
            <BookOpen className="h-3 w-3 text-primary-600" />
            <span>Prepare</span>
          </Link>

          {isTracked ? (
            <Link
              to="/applications"
              className="btn-secondary btn btn-xs text-emerald-700 border-emerald-300 bg-emerald-50 inline-flex items-center gap-1"
            >
              <Check className="h-3 w-3 text-emerald-600" />
              <span>Tracked</span>
            </Link>
          ) : (
            <button
              onClick={handleTrackApplication}
              disabled={tracking}
              className="btn-secondary btn btn-xs text-surface-700 hover:text-primary-700 inline-flex items-center gap-1"
              title="Track in application pipeline"
            >
              <ClipboardList className="h-3 w-3 text-surface-500" />
              <span>{tracking ? 'Saving...' : 'Track'}</span>
            </button>
          )}
        </div>

        <button
          onClick={() => onAnalyze(job.id)}
          disabled={isAnalyzing}
          className={`btn btn-xs inline-flex items-center gap-1 ${
            hasMatch ? 'btn-secondary text-primary-700 border-primary-200' : 'btn-primary'
          }`}
        >
          <Sparkles className="h-3 w-3" />
          <span>
            {isAnalyzing
              ? 'Analyzing...'
              : hasMatch
              ? 'View Match'
              : 'Analyze Match'}
          </span>
        </button>
      </div>
    </div>
  )
}
