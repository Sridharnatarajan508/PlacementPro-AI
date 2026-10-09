import { useState } from 'react'
import { X, CheckCircle, AlertTriangle, Sparkles, Building2, MapPin, Briefcase, IndianRupee, ExternalLink } from 'lucide-react'
import MatchScore, { formatRecommendation, getScoreTheme } from './MatchScore'

export default function JobMatchDetails({ job, match, loading, onClose, onAnalyze }) {
  if (!job) return null

  const formatSalary = (min, max) => {
    if (!min && !max) return 'Not disclosed'
    const toLPA = (val) => (val ? (Number(val) / 100000).toFixed(1) + ' LPA' : '')
    if (min && max) return `₹${toLPA(min)} - ₹${toLPA(max)}`
    if (min) return `From ₹${toLPA(min)}`
    return `Up to ₹${toLPA(max)}`
  }

  const reqSkillsList = job.requiredSkills ? job.requiredSkills.split(',').map(s => s.trim()).filter(Boolean) : []
  const prefSkillsList = job.preferredSkills ? job.preferredSkills.split(',').map(s => s.trim()).filter(Boolean) : []

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-surface-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-2xl animate-scale-in overflow-hidden border border-surface-200">
        {/* Header */}
        <div className="p-6 border-b border-surface-200 bg-surface-50 flex items-start justify-between">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <span className="badge-primary px-2.5 py-0.5 text-xs font-semibold rounded-md">
                {job.jobType ? job.jobType.replace('_', ' ') : 'Full Time'}
              </span>
              <span className="badge-secondary px-2.5 py-0.5 text-xs font-semibold rounded-md">
                {job.experienceLevel || 'Fresher'}
              </span>
            </div>
            <h2 className="text-xl font-bold text-surface-900">{job.title}</h2>
            <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-surface-500 pt-1">
              <span className="flex items-center gap-1 font-medium text-surface-700">
                <Building2 className="h-3.5 w-3.5 text-surface-400" />
                {job.company}
              </span>
              <span className="flex items-center gap-1">
                <MapPin className="h-3.5 w-3.5 text-surface-400" />
                {job.location}
              </span>
              <span className="flex items-center gap-1 font-medium text-emerald-700">
                <IndianRupee className="h-3.5 w-3.5 text-emerald-600" />
                {formatSalary(job.salaryMin, job.salaryMax)}
              </span>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-surface-400 hover:text-surface-700 hover:bg-surface-200 transition"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Scrollable Body */}
        <div className="p-6 overflow-y-auto space-y-6 flex-1">
          {/* AI Match Result Section */}
          {match ? (
            <div className="space-y-4">
              <div className="flex items-center gap-2">
                <Sparkles className="h-5 w-5 text-primary-600" />
                <h3 className="font-bold text-surface-900 text-base">AI Compatibility Analysis</h3>
              </div>

              <MatchScore
                overallScore={match.overallScore}
                skillScore={match.skillScore}
                experienceScore={match.experienceScore}
                educationScore={match.educationScore}
                recommendation={match.recommendation}
              />

              {/* AI Explanation */}
              {match.aiExplanation && (
                <div className="bg-primary-50/70 border border-primary-100 rounded-xl p-4">
                  <h4 className="text-xs font-bold text-primary-900 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                    <Sparkles className="h-3.5 w-3.5 text-primary-600" />
                    AI Fit Assessment
                  </h4>
                  <p className="text-xs text-primary-950 leading-relaxed">{match.aiExplanation}</p>
                </div>
              )}

              {/* Matching & Missing Skills */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {/* Matching Skills */}
                <div className="bg-emerald-50/60 border border-emerald-200 rounded-xl p-4 space-y-2">
                  <div className="flex items-center gap-1.5 text-emerald-800 text-xs font-bold uppercase tracking-wider">
                    <CheckCircle className="h-4 w-4 text-emerald-600" />
                    <span>Matching Skills ({match.matchingSkills?.length || 0})</span>
                  </div>
                  {match.matchingSkills && match.matchingSkills.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {match.matchingSkills.map((skill, idx) => (
                        <span key={idx} className="bg-white border border-emerald-300 text-emerald-800 text-xs px-2.5 py-1 rounded-md font-medium shadow-2xs">
                          {skill}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-surface-500 italic">No matching skills detected from current profile.</p>
                  )}
                </div>

                {/* Missing Skills */}
                <div className="bg-amber-50/60 border border-amber-200 rounded-xl p-4 space-y-2">
                  <div className="flex items-center gap-1.5 text-amber-800 text-xs font-bold uppercase tracking-wider">
                    <AlertTriangle className="h-4 w-4 text-amber-600" />
                    <span>Missing / Gap Skills ({match.missingSkills?.length || 0})</span>
                  </div>
                  {match.missingSkills && match.missingSkills.length > 0 ? (
                    <div className="flex flex-wrap gap-1.5 pt-1">
                      {match.missingSkills.map((skill, idx) => (
                        <span key={idx} className="bg-white border border-amber-300 text-amber-800 text-xs px-2.5 py-1 rounded-md font-medium shadow-2xs">
                          {skill}
                        </span>
                      ))}
                    </div>
                  ) : (
                    <p className="text-xs text-emerald-700 font-medium">Great! No critical missing skills detected.</p>
                  )}
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-surface-50 border border-dashed border-surface-300 rounded-xl p-6 text-center space-y-3">
              <div className="w-12 h-12 rounded-full bg-primary-100 text-primary-600 flex items-center justify-center mx-auto">
                <Sparkles className="h-6 w-6" />
              </div>
              <div>
                <h3 className="font-bold text-surface-900 text-sm">Analyze AI Compatibility</h3>
                <p className="text-xs text-surface-500 max-w-md mx-auto mt-1">
                  Evaluate how well your resume skills, education, and target role match this specific job opening.
                </p>
              </div>
              <button
                onClick={() => onAnalyze(job.id)}
                disabled={loading}
                className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
              >
                <Sparkles className="h-3.5 w-3.5" />
                {loading ? 'Analyzing with Gemini AI...' : 'Analyze My Match'}
              </button>
            </div>
          )}

          {/* Job Description */}
          <div className="space-y-2 pt-2 border-t border-surface-100">
            <h3 className="font-bold text-surface-800 text-sm">Job Description</h3>
            <p className="text-xs text-surface-600 leading-relaxed whitespace-pre-line bg-surface-50 p-4 rounded-xl border border-surface-100">
              {job.description}
            </p>
          </div>

          {/* Required Skills */}
          <div className="space-y-2">
            <h3 className="font-bold text-surface-800 text-sm">Required Skills</h3>
            <div className="flex flex-wrap gap-1.5">
              {reqSkillsList.map((skill, i) => (
                <span key={i} className="badge-surface px-2.5 py-1 text-xs rounded-md font-medium">
                  {skill}
                </span>
              ))}
            </div>
          </div>

          {/* Preferred Skills */}
          {prefSkillsList.length > 0 && (
            <div className="space-y-2">
              <h3 className="font-bold text-surface-800 text-sm">Preferred / Nice-to-Have Skills</h3>
              <div className="flex flex-wrap gap-1.5">
                {prefSkillsList.map((skill, i) => (
                  <span key={i} className="bg-surface-100 text-surface-700 px-2.5 py-1 text-xs rounded-md font-medium border border-surface-200">
                    {skill}
                  </span>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-surface-200 bg-surface-50 flex items-center justify-between">
          <button onClick={onClose} className="btn-secondary btn btn-sm">
            Close
          </button>
          {job.applicationUrl && (
            <a
              href={job.applicationUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
            >
              Apply on Company Site
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
          )}
        </div>
      </div>
    </div>
  )
}
