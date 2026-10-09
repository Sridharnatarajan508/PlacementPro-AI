import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { careerAnalyticsService, jobApplicationService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import {
  TrendingUp, Sparkles, RefreshCw, Award, AlertTriangle, CheckCircle2,
  BookOpen, Mic, Briefcase, Zap, Map, ArrowRight, Target, Calendar,
  BarChart3, Activity, Compass, ShieldCheck, ChevronRight, FileText,
  ClipboardList,Bot
} from 'lucide-react'
import toast from 'react-hot-toast'

export default function CareerAnalyticsPage() {
  const navigate = useNavigate()
  const [analytics, setAnalytics] = useState(null)
  const [appStats, setAppStats] = useState(null)
  const [loading, setLoading] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [error, setError] = useState(null)

  useEffect(() => {
    fetchAnalytics()
  }, [])

  const fetchAnalytics = async () => {
    setLoading(true)
    setError(null)
    try {
      const [res, statsRes] = await Promise.all([
        careerAnalyticsService.getAnalytics(),
        jobApplicationService.getStats().catch(() => ({ data: null }))
      ])
      setAnalytics(res.data)
      if (statsRes.data) setAppStats(statsRes.data)
    } catch (err) {
      if (err.response?.status === 404) {
        // Not generated yet - try initial generation
        handleInitialGenerate()
        return
      }
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const handleInitialGenerate = async () => {
    setGenerating(true)
    setError(null)
    try {
      const res = await careerAnalyticsService.generateAnalytics()
      setAnalytics(res.data)
      toast.success('Career Analytics Intelligence generated successfully!')
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setGenerating(false)
      setLoading(false)
    }
  }

  const handleRegenerate = async () => {
    if (generating) return
    setGenerating(true)
    setError(null)
    try {
      const res = await careerAnalyticsService.regenerateAnalytics()
      setAnalytics(res.data)
      toast.success('Career Analytics Intelligence refreshed!')
    } catch (err) {
      const errMsg = getErrorMessage(err)
      setError(errMsg)
      toast.error(errMsg)
    } finally {
      setGenerating(false)
    }
  }

  // Helpers
  const getScoreColor = (score) => {
    if (score >= 80) return 'text-emerald-600 stroke-emerald-500'
    if (score >= 60) return 'text-blue-600 stroke-blue-500'
    if (score >= 40) return 'text-amber-600 stroke-amber-500'
    return 'text-rose-600 stroke-rose-500'
  }

  const getLevelBadgeClass = (level) => {
    switch (level) {
      case 'READY':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200'
      case 'ALMOST READY':
        return 'bg-blue-50 text-blue-700 border-blue-200'
      case 'NEEDS IMPROVEMENT':
        return 'bg-amber-50 text-amber-700 border-amber-200'
      case 'NEEDS PREPARATION':
      default:
        return 'bg-rose-50 text-rose-700 border-rose-200'
    }
  }

  const getPriorityBadgeClass = (priority) => {
    switch (priority?.toUpperCase()) {
      case 'HIGH':
        return 'bg-rose-50 text-rose-700 border-rose-200'
      case 'MEDIUM':
        return 'bg-amber-50 text-amber-700 border-amber-200'
      case 'LOW':
      default:
        return 'bg-slate-50 text-slate-700 border-slate-200'
    }
  }

  const getFeatureRoute = (feature) => {
    switch (feature?.toUpperCase()) {
      case 'SKILL_GAP':
        return '/skill-gap'
      case 'CAREER_ROADMAP':
        return '/career-roadmap'
      case 'MOCK_INTERVIEW':
        return '/mock-interview'
      case 'JOB_PREPARATION':
        return '/preparation'
      case 'RESUME_ANALYZER':
        return '/resume'
      case 'JOB_MATCHER':
      default:
        return '/jobs'
    }
  }

  const getFeatureLabel = (feature) => {
    switch (feature?.toUpperCase()) {
      case 'SKILL_GAP':
        return 'Skill Gap Analysis'
      case 'CAREER_ROADMAP':
        return 'Career Roadmap'
      case 'MOCK_INTERVIEW':
        return 'Mock Interview'
      case 'JOB_PREPARATION':
        return 'Job Preparation'
      case 'RESUME_ANALYZER':
        return 'Resume Analyzer'
      case 'JOB_MATCHER':
      default:
        return 'Job Matcher'
    }
  }

  const score = analytics?.overallReadinessScore || 0
  const radius = 42
  const circumference = 2 * Math.PI * radius
  const strokeDashoffset = circumference - (score / 100) * circumference

  return (
    <AppLayout>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <div className="p-2 bg-indigo-50 rounded-lg text-indigo-600">
                <TrendingUp className="h-6 w-6" />
              </div>
              <h1 className="text-2xl font-bold text-slate-900">AI Career Analytics & Intelligence</h1>
            </div>
            <p className="mt-1 text-sm text-slate-500">
              Consolidated intelligence dashboard evaluating your complete readiness across skills, resume, interviews, and goals.
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={handleRegenerate}
              disabled={generating || loading}
              className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 rounded-lg shadow-sm disabled:opacity-50 transition-colors"
            >
              <RefreshCw className={`h-4 w-4 ${generating ? 'animate-spin' : ''}`} />
              {generating ? 'Evaluating AI Intelligence...' : 'Regenerate Analysis'}
            </button>
          </div>
        </div>

        {/* Error Alert */}
        {error && (
          <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-rose-800 flex items-start gap-3">
            <AlertTriangle className="h-5 w-5 text-rose-600 flex-shrink-0 mt-0.5" />
            <div className="flex-1 text-sm">
              <p className="font-semibold text-rose-900">Analysis Notice</p>
              <p className="mt-0.5">{error}</p>
            </div>
            <button
              onClick={handleRegenerate}
              className="text-xs font-semibold px-3 py-1.5 bg-white border border-rose-200 text-rose-700 hover:bg-rose-100 rounded-md transition-colors"
            >
              Retry
            </button>
          </div>
        )}

        {/* Loading State */}
        {loading && !analytics && (
          <div className="flex flex-col items-center justify-center py-20 bg-white rounded-2xl border border-slate-200">
            <LoadingSpinner size="lg" />
            <p className="mt-4 text-sm font-medium text-slate-600">
              Gathering career intelligence across your resume, skills, and mock interviews...
            </p>
          </div>
        )}

        {/* Analytics Content */}
        {!loading && analytics && (
          <>
            {/* Section A: Career Readiness Score Card */}
            <div className="bg-white rounded-2xl border border-slate-200 p-6 lg:p-8 shadow-sm">
              <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
                {/* Score Gauge */}
                <div className="lg:col-span-4 flex flex-col items-center justify-center text-center p-4 border-b lg:border-b-0 lg:border-r border-slate-100">
                  <div className="relative w-36 h-36 flex items-center justify-center">
                    <svg className="w-full h-full transform -rotate-90" viewBox="0 0 100 100">
                      <circle
                        cx="50"
                        cy="50"
                        r={radius}
                        className="stroke-slate-100"
                        strokeWidth="10"
                        fill="transparent"
                      />
                      <circle
                        cx="50"
                        cy="50"
                        r={radius}
                        className={`transition-all duration-1000 ease-out ${getScoreColor(score)}`}
                        strokeWidth="10"
                        strokeDasharray={circumference}
                        strokeDashoffset={strokeDashoffset}
                        strokeLinecap="round"
                        fill="transparent"
                      />
                    </svg>
                    <div className="absolute flex flex-col items-center justify-center">
                      <span className="text-3xl font-extrabold text-slate-900">{score}%</span>
                      <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-400">Readiness</span>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-col items-center gap-1.5">
                    <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-bold border ${getLevelBadgeClass(analytics.readinessLevel)}`}>
                      {analytics.readinessLevel || 'NEEDS PREPARATION'}
                    </span>
                    {analytics.previousReadinessScore !== null && analytics.previousReadinessScore !== undefined && (
                      <span className="text-xs text-slate-500 font-medium">
                        Previous: {analytics.previousReadinessScore}% (
                        {score >= analytics.previousReadinessScore ? (
                          <span className="text-emerald-600 font-semibold">+{score - analytics.previousReadinessScore}%</span>
                        ) : (
                          <span className="text-rose-600 font-semibold">{score - analytics.previousReadinessScore}%</span>
                        )}
                        )
                      </span>
                    )}
                    <span className="text-[11px] text-slate-400 mt-1">
                      Updated: {analytics.updatedAt ? new Date(analytics.updatedAt).toLocaleDateString() : 'Just now'}
                    </span>
                  </div>
                </div>

                {/* AI Career Summary */}
                <div className="lg:col-span-8 space-y-3">
                  <div className="flex items-center gap-2 text-indigo-600">
                    <Sparkles className="h-5 w-5" />
                    <h2 className="text-base font-bold text-slate-900">AI Career Intelligence Summary</h2>
                  </div>
                  <div className="text-sm text-slate-700 leading-relaxed bg-slate-50/70 rounded-xl p-4 border border-slate-100 whitespace-pre-line">
                    {analytics.careerSummary || 'Your career analytics summary will appear here.'}
                  </div>
                </div>
              </div>
            </div>

            {/* Section B & C: Strengths & Weaknesses */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {/* Strengths */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col">
                <div className="flex items-center gap-2 mb-4">
                  <div className="p-1.5 bg-emerald-50 rounded-md text-emerald-600">
                    <ShieldCheck className="h-5 w-5" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">Verified Core Strengths</h3>
                </div>
                {analytics.strengths && analytics.strengths.length > 0 ? (
                  <div className="flex flex-wrap gap-2">
                    {analytics.strengths.map((str, idx) => (
                      <div
                        key={idx}
                        className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-50/70 border border-emerald-200/80 text-emerald-800 text-xs font-semibold"
                      >
                        <CheckCircle2 className="h-3.5 w-3.5 text-emerald-600 flex-shrink-0" />
                        <span>{str}</span>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">No specific strengths recorded yet. Upload a resume to detect strengths.</p>
                )}
              </div>

              {/* Weaknesses / Priority Gaps */}
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm flex flex-col">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-2">
                    <div className="p-1.5 bg-rose-50 rounded-md text-rose-600">
                      <AlertTriangle className="h-5 w-5" />
                    </div>
                    <h3 className="text-base font-bold text-slate-900">Priority Skill & Readiness Gaps</h3>
                  </div>
                  <Link to="/skill-gap" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700">
                    View Skill Gap &rarr;
                  </Link>
                </div>
                {analytics.weaknesses && analytics.weaknesses.length > 0 ? (
                  <div className="space-y-3">
                    {analytics.weaknesses.map((w, idx) => (
                      <div key={idx} className="p-3 bg-slate-50 rounded-xl border border-slate-100 text-xs space-y-1">
                        <div className="flex items-center justify-between">
                          <span className="font-bold text-slate-900">{w.skill}</span>
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getPriorityBadgeClass(w.priority)}`}>
                            {w.priority || 'MEDIUM'}
                          </span>
                        </div>
                        {w.currentStatus && <p className="text-slate-600">{w.currentStatus}</p>}
                        {w.recommendedAction && (
                          <p className="text-indigo-700 font-medium">Action: {w.recommendedAction}</p>
                        )}
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-xs text-slate-400 italic">No priority weaknesses detected.</p>
                )}
              </div>
            </div>

            {/* Section D: Skill Progress Matrix */}
            {analytics.skillProgress && analytics.skillProgress.length > 0 && (
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <div className="p-1.5 bg-blue-50 rounded-md text-blue-600">
                    <Zap className="h-5 w-5" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">Skill Competency Progress</h3>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                  {analytics.skillProgress.map((sk, idx) => (
                    <div key={idx} className="p-3.5 rounded-xl border border-slate-100 bg-slate-50/50 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-bold text-slate-900">{sk.skill}</span>
                        <span className="text-[10px] uppercase font-bold text-slate-400">{sk.category}</span>
                      </div>
                      <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                        <div
                          className={`h-2 rounded-full ${
                            sk.status === 'STRONG'
                              ? 'bg-emerald-500'
                              : sk.status === 'DEVELOPING'
                              ? 'bg-blue-500'
                              : 'bg-amber-500'
                          }`}
                          style={{ width: `${sk.proficiencyPercentage || 50}%` }}
                        />
                      </div>
                      <div className="flex items-center justify-between text-[10px] text-slate-500 font-medium">
                        <span>{sk.status || 'DEVELOPING'}</span>
                        <span>{sk.proficiencyPercentage || 50}%</span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Section E, F, G: Activity & Progress Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {/* Application Funnel Performance */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-blue-50 rounded-md text-blue-600">
                        <ClipboardList className="h-4 w-4" />
                      </div>
                      <h4 className="text-xs font-bold text-slate-900">Application Funnel</h4>
                    </div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-blue-50 text-blue-700">
                      {appStats?.activeCount ? `${appStats.activeCount} ACTIVE` : 'TRACKED'}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mb-3">
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Applications</span>
                      <span className="text-base font-extrabold text-slate-900">
                        {appStats?.totalApplications || analytics.jobSearchProgress?.appliedJobs || 0}
                      </span>
                    </div>
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Response Rate</span>
                      <span className="text-base font-extrabold text-blue-700">
                        {appStats?.responseRate != null ? `${appStats.responseRate}%` : '0%'}
                      </span>
                    </div>
                  </div>

                  <div className="space-y-1 text-[11px] text-slate-600">
                    <div className="flex justify-between">
                      <span>Interview Rate:</span>
                      <span className="font-semibold">{appStats?.interviewRate || 0}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Offer Rate:</span>
                      <span className="font-semibold text-emerald-700">{appStats?.offerRate || 0}%</span>
                    </div>
                  </div>
                </div>

                <Link
                  to="/applications"
                  className="mt-3 w-full text-center text-xs font-semibold py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg border border-slate-200 transition-colors inline-flex items-center justify-center gap-1"
                >
                  Application Tracker <ChevronRight className="h-3.5 w-3.5" />
                </Link>
              </div>

              {/* Interview Performance */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-rose-50 rounded-md text-rose-600">
                        <Mic className="h-4 w-4" />
                      </div>
                      <h4 className="text-xs font-bold text-slate-900">Mock Interviews</h4>
                    </div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-rose-50 text-rose-700">
                      {analytics.interviewProgress?.trend || 'ACTIVE'}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mb-3">
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Avg Score</span>
                      <span className="text-base font-extrabold text-slate-900">
                        {Math.round(analytics.interviewProgress?.averageScore || 0)}%
                      </span>
                    </div>
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Completed</span>
                      <span className="text-base font-extrabold text-slate-900">
                        {analytics.interviewProgress?.totalInterviews || 0}
                      </span>
                    </div>
                  </div>

                  <div className="space-y-1 text-[11px] text-slate-600">
                    <div className="flex justify-between">
                      <span>Technical:</span>
                      <span className="font-semibold">{Math.round(analytics.interviewProgress?.technicalScore || 0)}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Behavioral:</span>
                      <span className="font-semibold">{Math.round(analytics.interviewProgress?.behavioralScore || 0)}%</span>
                    </div>
                  </div>
                </div>

                <Link
                  to="/mock-interview"
                  className="mt-3 w-full text-center text-xs font-semibold py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg border border-slate-200 transition-colors inline-flex items-center justify-center gap-1"
                >
                  Practice Interview <ChevronRight className="h-3.5 w-3.5" />
                </Link>
              </div>

              {/* Job Search Progress */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-purple-50 rounded-md text-purple-600">
                        <Briefcase className="h-4 w-4" />
                      </div>
                      <h4 className="text-xs font-bold text-slate-900">Job Matching</h4>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-2 mb-3">
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Matched</span>
                      <span className="text-base font-extrabold text-slate-900">
                        {analytics.jobSearchProgress?.jobsMatched || 0}
                      </span>
                    </div>
                    <div className="p-2 bg-slate-50 rounded-lg text-center">
                      <span className="text-[9px] text-slate-400 font-semibold uppercase block">Saved</span>
                      <span className="text-base font-extrabold text-slate-900">
                        {analytics.jobSearchProgress?.savedJobs || 0}
                      </span>
                    </div>
                  </div>

                  <div className="space-y-1 text-[11px] text-slate-600">
                    <div className="flex justify-between">
                      <span>Avg Match Score:</span>
                      <span className="font-semibold">{Math.round(analytics.jobSearchProgress?.avgMatchScore || 0)}%</span>
                    </div>
                    <div className="flex justify-between">
                      <span>Prep Sessions:</span>
                      <span className="font-semibold">{analytics.jobSearchProgress?.preparationSessions || 0}</span>
                    </div>
                  </div>
                </div>

                <Link
                  to="/jobs"
                  className="mt-3 w-full text-center text-xs font-semibold py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg border border-slate-200 transition-colors inline-flex items-center justify-center gap-1"
                >
                  Explore Jobs <ChevronRight className="h-3.5 w-3.5" />
                </Link>
              </div>

              {/* Career Roadmap Progress */}
              <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between">
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 bg-green-50 rounded-md text-green-600">
                        <Map className="h-4 w-4" />
                      </div>
                      <h4 className="text-xs font-bold text-slate-900">Roadmap Milestone</h4>
                    </div>
                    <span className="text-[10px] font-bold text-green-700">
                      {analytics.roadmapProgress?.completionPercentage || 0}% Done
                    </span>
                  </div>

                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden mb-3">
                    <div
                      className="bg-green-500 h-2 rounded-full transition-all duration-500"
                      style={{ width: `${analytics.roadmapProgress?.completionPercentage || 0}%` }}
                    />
                  </div>

                  <div className="space-y-1.5 text-[11px]">
                    <div>
                      <span className="text-[9px] uppercase font-bold text-slate-400 block">Current Milestone</span>
                      <p className="font-semibold text-slate-800 truncate">
                        {analytics.roadmapProgress?.currentMilestone || 'In Progress'}
                      </p>
                    </div>
                    {analytics.roadmapProgress?.upcomingMilestone && (
                      <div>
                        <span className="text-[9px] uppercase font-bold text-slate-400 block">Upcoming</span>
                        <p className="text-slate-600 truncate">{analytics.roadmapProgress?.upcomingMilestone}</p>
                      </div>
                    )}
                  </div>
                </div>

                <Link
                  to="/career-roadmap"
                  className="mt-3 w-full text-center text-xs font-semibold py-2 bg-slate-50 hover:bg-slate-100 text-slate-700 rounded-lg border border-slate-200 transition-colors inline-flex items-center justify-center gap-1"
                >
                  Continue Roadmap <ChevronRight className="h-3.5 w-3.5" />
                </Link>
              </div>
              </div>
            {/* Section H: AI Recommended Next Actions */}
            {analytics.recommendations && analytics.recommendations.length > 0 && (
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <div className="p-1.5 bg-indigo-50 rounded-md text-indigo-600">
                    <Target className="h-5 w-5" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">AI Recommended Next Actions</h3>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {analytics.recommendations.map((rec, idx) => (
                    <div
                      key={idx}
                      className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition-colors flex flex-col justify-between gap-3"
                    >
                      <div className="space-y-1.5">
                        <div className="flex items-center justify-between">
                          <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${getPriorityBadgeClass(rec.priority)}`}>
                            {rec.priority || 'MEDIUM'}
                          </span>
                          <span className="text-[10px] text-slate-400 font-semibold">{getFeatureLabel(rec.relatedFeature)}</span>
                        </div>
                        <h4 className="text-sm font-bold text-slate-900">{rec.action}</h4>
                        <p className="text-xs text-slate-600">{rec.reason}</p>
                      </div>

                      <Link
                        to={getFeatureRoute(rec.relatedFeature)}
                        className="inline-flex items-center gap-1.5 text-xs font-bold text-indigo-600 hover:text-indigo-700 self-start"
                      >
                        Start Action <ArrowRight className="h-3.5 w-3.5" />
                      </Link>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Section I: 7-Day Weekly Action Goals */}
            {analytics.weeklyGoals && analytics.weeklyGoals.length > 0 && (
              <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm">
                <div className="flex items-center gap-2 mb-4">
                  <div className="p-1.5 bg-amber-50 rounded-md text-amber-600">
                    <Calendar className="h-5 w-5" />
                  </div>
                  <h3 className="text-base font-bold text-slate-900">7-Day Targeted Action Plan</h3>
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
                  {analytics.weeklyGoals.map((goal, idx) => (
                    <div key={idx} className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-extrabold text-indigo-600">{goal.day || `Day ${idx + 1}`}</span>
                        {goal.focusArea && (
                          <span className="text-[10px] px-2 py-0.5 rounded bg-slate-200/60 text-slate-700 font-semibold">
                            {goal.focusArea}
                          </span>
                        )}
                      </div>
                      <h4 className="text-xs font-bold text-slate-900">{goal.title}</h4>
                      <p className="text-[11px] text-slate-600 leading-relaxed">{goal.description}</p>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Section J: Ask AI About My Analytics */}
            <div className="bg-gradient-to-r from-indigo-50/70 via-white to-purple-50/70 rounded-2xl border border-indigo-100 p-6 shadow-sm">
              <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 bg-indigo-600 text-white rounded-xl shadow-xs">
                    <Sparkles className="h-6 w-6" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-slate-900">Ask AI About Your Career Analytics</h3>
                    <p className="text-xs text-slate-600 mt-0.5">
                      Get real-time conversational explanations, deep-dives into your weaknesses, and tailored strategies.
                    </p>
                  </div>
                </div>

                <div className="flex flex-wrap gap-2 w-full md:w-auto">
                  <Link
                    to="/ai-assistant?prompt=Explain my career readiness score and what is currently holding me back from reaching the READY level."
                    className="btn-secondary btn btn-xs inline-flex items-center gap-1.5"
                  >
                    <span>Explain My Score</span>
                    <ArrowRight className="h-3 w-3" />
                  </Link>
                  <Link
                    to="/ai-assistant?prompt=What should I improve first based on my priority weaknesses and skill gaps?"
                    className="btn-secondary btn btn-xs inline-flex items-center gap-1.5"
                  >
                    <span>What to Improve First?</span>
                    <ArrowRight className="h-3 w-3" />
                  </Link>
                  <Link
                    to="/ai-assistant?prompt=Create a customized 7-day study and mock interview plan based on my career analytics."
                    className="btn-primary btn btn-xs inline-flex items-center gap-1.5 shadow-xs"
                  >
                    <Bot className="h-3.5 w-3.5" />
                    <span>Create 7-Day Plan</span>
                  </Link>
                </div>
              </div>
            </div>
          </>
        )}
      </div>
    </AppLayout>
  )
}
