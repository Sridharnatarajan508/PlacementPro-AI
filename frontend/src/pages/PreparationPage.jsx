import { useState, useEffect, useCallback } from 'react'
import { useSearchParams, useNavigate, Link } from 'react-router-dom'
import { toast } from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { jobService, jobPreparationService, jobApplicationService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'

import {
  BookOpen, Sparkles, Building2, MapPin, Briefcase,
  CheckCircle2, AlertCircle, RefreshCw, ChevronDown,
  ChevronUp, Mic, ArrowRight, Code, Layers, MessageSquare,
  Award, Clock, CheckSquare, Target, HelpCircle, FileText,
  ClipboardList
} from 'lucide-react'

export default function PreparationPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const navigate = useNavigate()
  const queryJobId = searchParams.get('jobId')

  const [jobs, setJobs] = useState([])
  const [selectedJobId, setSelectedJobId] = useState(queryJobId ? Number(queryJobId) : null)
  const [selectedJob, setSelectedJob] = useState(null)
  const [preparation, setPreparation] = useState(null)
  const [loadingJobs, setLoadingJobs] = useState(true)
  const [loadingPrep, setLoadingPrep] = useState(false)
  const [generating, setGenerating] = useState(false)
  const [tracking, setTracking] = useState(false)

  const [activeTab, setActiveTab] = useState('plan') // 'plan' | 'topics' | 'questions' | 'skills'
  const [expandedQuestionIdx, setExpandedQuestionIdx] = useState(null)

  // 1. Fetch available jobs list
  useEffect(() => {
    const fetchJobs = async () => {
      setLoadingJobs(true)
      try {
        const { data } = await jobService.getJobs()
        const jobList = Array.isArray(data) ? data : data.content || []
        setJobs(jobList)

        if (jobList.length > 0) {
          if (queryJobId) {
            const found = jobList.find(j => j.id === Number(queryJobId))
            if (found) {
              setSelectedJobId(found.id)
              setSelectedJob(found)
            } else {
              setSelectedJobId(jobList[0].id)
              setSelectedJob(jobList[0])
            }
          } else {
            setSelectedJobId(jobList[0].id)
            setSelectedJob(jobList[0])
          }
        }
      } catch (err) {
        console.error('Failed to load jobs:', err)
        toast.error('Failed to load job listings.')
      } finally {
        setLoadingJobs(false)
      }
    }

    fetchJobs()
  }, [queryJobId])

  // 2. Fetch preparation for selected job
  const loadPreparation = useCallback(async (jobId) => {
    if (!jobId) return
    setLoadingPrep(true)
    try {
      const { data } = await jobPreparationService.get(jobId)
      setPreparation(data)
    } catch (err) {
      if (err.response?.status === 404) {
        setPreparation(null)
      } else {
        console.error('Failed to load job preparation:', err)
      }
    } finally {
      setLoadingPrep(false)
    }
  }, [])

  useEffect(() => {
    if (selectedJobId) {
      loadPreparation(selectedJobId)
      const current = jobs.find(j => j.id === selectedJobId)
      if (current) setSelectedJob(current)
    }
  }, [selectedJobId, jobs, loadPreparation])

  // Switch Job Handler
  const handleJobSelect = (id) => {
    const numId = Number(id)
    setSelectedJobId(numId)
    setSearchParams({ jobId: numId })
  }

  // Generate / Regenerate
  const handleGenerate = async (forceRegenerate = false) => {
    if (!selectedJobId) return
    setGenerating(true)
    try {
      const apiCall = forceRegenerate
        ? jobPreparationService.regenerate(selectedJobId)
        : jobPreparationService.generate(selectedJobId)

      const { data } = await apiCall
      setPreparation(data)
      toast.success(forceRegenerate ? 'Preparation plan regenerated!' : 'Preparation plan generated successfully!')
    } catch (err) {
      const status = err.response?.status
      if (status === 401) {
        toast.error('Session expired. Please log in again.')
      } else if (status === 403) {
        toast.error('You are not authorized to access this preparation.')
      } else if (status === 404) {
        toast.error('No preparation plan found.')
      } else if (status === 429) {
        toast.error('AI service is temporarily busy. Please try again later.')
      } else if (status >= 500) {
        toast.error('Unable to generate preparation plan right now.')
      } else {
        toast.error(getErrorMessage(err) || 'Failed to generate preparation plan.')
      }
    } finally {
      setGenerating(false)
    }
  }

  const handleTrackJob = async () => {
    if (!selectedJobId || tracking) return
    setTracking(true)
    try {
      await jobApplicationService.createApplication(selectedJobId)
      toast.success('Job added to your Application Tracker pipeline!')
    } catch (err) {
      const msg = getErrorMessage(err)
      if (msg && msg.toLowerCase().includes('already')) {
        toast.info('Job is already tracked in your Application Tracker.')
      } else {
        toast.error(msg || 'Could not track application.')
      }
    } finally {
      setTracking(false)
    }
  }

  // Readiness badge helper
  const getReadinessLevel = (score) => {
    if (score >= 80) return { label: 'Ready', color: 'text-emerald-700 bg-emerald-50 border-emerald-300' }
    if (score >= 60) return { label: 'Almost Ready', color: 'text-amber-700 bg-amber-50 border-amber-300' }
    return { label: 'Needs Preparation', color: 'text-rose-700 bg-rose-50 border-rose-300' }
  }

  const getQuestionTypeBadge = (type) => {
    const t = (type || '').toUpperCase()
    if (t === 'TECHNICAL') return 'bg-blue-50 text-blue-700 border-blue-200'
    if (t === 'CODING') return 'bg-purple-50 text-purple-700 border-purple-200'
    if (t === 'BEHAVIORAL') return 'bg-amber-50 text-amber-700 border-amber-200'
    return 'bg-emerald-50 text-emerald-700 border-emerald-200'
  }

  return (
    <AppLayout>
      <div className="space-y-6 animate-slide-up pb-12">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-surface-900 flex items-center gap-2">
              <BookOpen className="h-6 w-6 text-primary-600" />
              AI Job Preparation
            </h1>
            <p className="text-sm text-surface-500 mt-1">
              Your personalized preparation plan for the job you want.
            </p>
          </div>

          {/* Job Picker */}
          {jobs.length > 0 && (
            <div className="flex items-center gap-2 flex-wrap">
              <label className="text-xs font-semibold text-surface-500">Target Opening:</label>
              <select
                value={selectedJobId || ''}
                onChange={(e) => handleJobSelect(e.target.value)}
                className="input py-1.5 px-3 text-xs font-semibold bg-white border border-surface-300 rounded-lg shadow-xs max-w-xs"
              >
                {jobs.map(j => (
                  <option key={j.id} value={j.id}>
                    {j.title} — {j.company}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {loadingJobs ? (
          <LoadingSpinner text="Loading jobs and preparation data..." />
        ) : jobs.length === 0 ? (
          <EmptyState
            icon={<Briefcase className="text-primary-500" />}
            title="No Jobs Available"
            description="No job postings were found in the database. Please visit the Job Matcher to explore available roles."
            action={
              <Link to="/jobs" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                <span>Go to Job Matcher</span>
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            }
          />
        ) : (
          <div className="space-y-6">
            {/* Top Selected Job & Readiness Card */}
            {selectedJob && (
              <div className="card p-6 border border-surface-200 bg-white shadow-xs">
                <div className="grid grid-cols-1 lg:grid-cols-4 gap-6 items-center">
                  {/* Readiness Card */}
                  <div className="flex items-center gap-4 border-b lg:border-b-0 lg:border-r border-surface-100 pb-4 lg:pb-0 pr-0 lg:pr-4">
                    <div className={`w-20 h-20 rounded-2xl border-2 flex flex-col items-center justify-center font-bold shadow-xs ${
                      preparation ? getReadinessLevel(preparation.readinessScore || 0).color : 'bg-surface-50 text-surface-400 border-surface-200'
                    }`}>
                      <span className="text-2xl leading-none">
                        {preparation ? `${preparation.readinessScore}%` : '—'}
                      </span>
                      <span className="text-[10px] uppercase font-bold tracking-wider mt-1 opacity-80">
                        READINESS
                      </span>
                    </div>
                    <div>
                      <span className="text-[11px] font-bold uppercase tracking-wider text-surface-400 block">
                        Job Readiness
                      </span>
                      <h3 className="text-sm font-bold text-surface-900">
                        {preparation ? getReadinessLevel(preparation.readinessScore || 0).label : 'Not Analyzed'}
                      </h3>
                      <span className="text-xs text-surface-500">
                        {preparation ? `Level: ${preparation.preparationLevel}` : 'Generate plan to assess'}
                      </span>
                    </div>
                  </div>

                  {/* Job Details */}
                  <div className="lg:col-span-2 space-y-2">
                    <div className="flex items-center gap-2 flex-wrap">
                      <h2 className="text-lg font-bold text-surface-900 leading-tight">
                        {selectedJob.title}
                      </h2>
                      <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-primary-50 text-primary-700 border border-primary-200">
                        {selectedJob.company}
                      </span>
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-surface-500">
                      <span className="flex items-center gap-1">
                        <MapPin className="h-3.5 w-3.5 text-surface-400" />
                        {selectedJob.location || 'Remote'}
                      </span>
                      <span className="flex items-center gap-1">
                        <Briefcase className="h-3.5 w-3.5 text-surface-400" />
                        {selectedJob.experienceLevel || 'Fresher / Entry Level'}
                      </span>
                      {selectedJob.matchScore != null && (
                        <span className="font-semibold text-emerald-700">
                          {selectedJob.matchScore}% Match
                        </span>
                      )}
                    </div>

                    {/* Section A: AI Preparation Summary */}
                    {preparation?.summary && (
                      <p className="text-xs text-surface-600 leading-relaxed pt-1">
                        {preparation.summary}
                      </p>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex flex-col items-start lg:items-end justify-center gap-2">
                    {preparation ? (
                      <button
                        onClick={() => handleGenerate(true)}
                        disabled={generating}
                        className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-sm"
                      >
                        <RefreshCw className={`h-3.5 w-3.5 ${generating ? 'animate-spin' : ''}`} />
                        <span>{generating ? 'Regenerating...' : 'Regenerate Plan'}</span>
                      </button>
                    ) : (
                      <button
                        onClick={() => handleGenerate(false)}
                        disabled={generating}
                        className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-sm"
                      >
                        <Sparkles className="h-3.5 w-3.5" />
                        <span>{generating ? 'Generating Plan...' : 'Generate AI Plan'}</span>
                      </button>
                    )}

                    <div className="flex flex-wrap items-center gap-2">
                      <button
                        onClick={handleTrackJob}
                        disabled={tracking}
                        className="btn-secondary btn btn-xs inline-flex items-center gap-1 text-emerald-700 border-emerald-200 hover:bg-emerald-50 font-semibold"
                        title="Track this job in Application Tracker"
                      >
                        <ClipboardList className="h-3 w-3 text-emerald-600" />
                        <span>{tracking ? 'Tracking...' : 'Track Application'}</span>
                      </button>

                      <Link
                        to={`/ai-assistant?prompt=${encodeURIComponent(`How should I prepare for the ${selectedJob.title} position at ${selectedJob.company}? What are the key skills, gaps, and interview strategies I need to focus on?`)}`}
                        className="btn-secondary btn btn-xs inline-flex items-center gap-1 text-primary-700 font-semibold"
                      >
                        <Sparkles className="h-3 w-3 text-primary-600" />
                        <span>Ask AI About This Job</span>
                      </Link>

                      <Link
                        to={`/mock-interview?targetRole=${encodeURIComponent(selectedJob.title)}`}
                        className="btn-secondary btn btn-xs inline-flex items-center gap-1 text-surface-700"
                      >
                        <Mic className="h-3 w-3 text-rose-500" />
                        <span>Practice Interview</span>
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* Preparation Content */}
            {loadingPrep ? (
              <LoadingSpinner text="Loading preparation plan..." />
            ) : !preparation ? (
              <EmptyState
                icon={<BookOpen className="text-primary-500" />}
                title="No Preparation Plan Generated Yet"
                description={`Generate an AI-powered 7–14 day preparation plan for ${selectedJob?.title} at ${selectedJob?.company} combining your verified skills, resume gaps, and technical questions.`}
                action={
                  <button
                    onClick={() => handleGenerate(false)}
                    disabled={generating}
                    className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
                  >
                    <Sparkles className="h-3.5 w-3.5" />
                    <span>{generating ? 'Analyzing with Gemini...' : 'Generate AI Preparation Plan'}</span>
                  </button>
                }
              />
            ) : (
              <div className="space-y-6">
                {/* Navigation Tabs */}
                <div className="flex items-center gap-2 border-b border-surface-200 pb-1 flex-wrap">
                  <button
                    onClick={() => setActiveTab('plan')}
                    className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                      activeTab === 'plan'
                        ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                        : 'text-surface-600 hover:text-surface-900'
                    }`}
                  >
                    <Clock className="h-3.5 w-3.5 text-primary-600" />
                    <span>Preparation Schedule ({preparation.preparationPlan?.length || 0} Days)</span>
                  </button>

                  <button
                    onClick={() => setActiveTab('topics')}
                    className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                      activeTab === 'topics'
                        ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                        : 'text-surface-600 hover:text-surface-900'
                    }`}
                  >
                    <Layers className="h-3.5 w-3.5 text-indigo-600" />
                    <span>Curriculum & Topics</span>
                  </button>

                  <button
                    onClick={() => setActiveTab('questions')}
                    className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                      activeTab === 'questions'
                        ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                        : 'text-surface-600 hover:text-surface-900'
                    }`}
                  >
                    <HelpCircle className="h-3.5 w-3.5 text-amber-600" />
                    <span>Interview Questions ({preparation.recommendedQuestions?.length || 0})</span>
                  </button>

                  <button
                    onClick={() => setActiveTab('skills')}
                    className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                      activeTab === 'skills'
                        ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                        : 'text-surface-600 hover:text-surface-900'
                    }`}
                  >
                    <Target className="h-3.5 w-3.5 text-emerald-600" />
                    <span>Strengths & Skill Gaps</span>
                  </button>
                </div>

                {/* TAB 1: Section I — 7–14 Day Preparation Plan */}
                {activeTab === 'plan' && (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="text-sm font-bold text-surface-900 flex items-center gap-2">
                        <Clock className="h-4 w-4 text-primary-600" />
                        Step-by-Step Daily Study & Practice Schedule
                      </h3>
                      <span className="text-xs text-surface-400">
                        {preparation.preparationPlan?.length || 0} Targeted Days
                      </span>
                    </div>

                    <div className="grid grid-cols-1 gap-4">
                      {(preparation.preparationPlan || []).map((day, idx) => (
                        <div key={idx} className="card p-5 border border-surface-200 hover:shadow-md transition space-y-4">
                          {/* Day Header */}
                          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-surface-100 pb-3">
                            <div className="flex items-center gap-3">
                              <div className="w-8 h-8 rounded-xl bg-primary-100 text-primary-800 font-bold text-xs flex items-center justify-center flex-shrink-0">
                                D{day.day || idx + 1}
                              </div>
                              <div>
                                <h4 className="font-bold text-surface-900 text-sm">{day.title}</h4>
                                <p className="text-xs text-surface-500 mt-0.5">{day.focus}</p>
                              </div>
                            </div>
                            <span className="badge-primary text-[11px] px-2.5 py-0.5 rounded-full self-start sm:self-center">
                              Day {day.day || idx + 1}
                            </span>
                          </div>

                          {/* Day Details */}
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
                            {/* Topics */}
                            <div className="space-y-1.5">
                              <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                                Core Study Topics
                              </span>
                              <div className="flex flex-wrap gap-1">
                                {(day.topics || []).map((t, i) => (
                                  <span key={i} className="px-2 py-0.5 rounded bg-surface-100 text-surface-700 font-medium">
                                    {t}
                                  </span>
                                ))}
                              </div>
                            </div>

                            {/* Actionable Tasks */}
                            <div className="space-y-1.5">
                              <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                                Practice Tasks
                              </span>
                              <ul className="space-y-1">
                                {(day.tasks || []).map((task, i) => (
                                  <li key={i} className="flex items-start gap-1.5 text-surface-700 font-medium">
                                    <CheckSquare className="h-3.5 w-3.5 text-emerald-600 mt-0.5 flex-shrink-0" />
                                    <span>{task}</span>
                                  </li>
                                ))}
                              </ul>
                            </div>

                            {/* Practice Questions */}
                            <div className="space-y-1.5">
                              <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                                Interview Practice
                              </span>
                              <div className="bg-primary-50/50 border border-primary-100 p-2.5 rounded-lg space-y-1">
                                {(day.practiceQuestions || []).map((q, i) => (
                                  <p key={i} className="text-primary-900 font-medium leading-relaxed">
                                    💬 {q}
                                  </p>
                                ))}
                              </div>
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* TAB 2: Sections D, E, F, G, H — Curriculum & Topics */}
                {activeTab === 'topics' && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Section D: Technical Topics */}
                    <div className="card p-5 border border-surface-200 space-y-3">
                      <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                        <Layers className="h-4 w-4 text-blue-600" />
                        <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                          Technical Topics
                        </h4>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {(preparation.technicalTopics || []).map((item, i) => (
                          <span key={i} className="px-2.5 py-1 rounded bg-blue-50 text-blue-800 border border-blue-200 text-xs font-medium">
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Section E: Coding Preparation */}
                    <div className="card p-5 border border-surface-200 space-y-3">
                      <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                        <Code className="h-4 w-4 text-purple-600" />
                        <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                          Coding & DSA Focus
                        </h4>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {(preparation.codingTopics || []).map((item, i) => (
                          <span key={i} className="px-2.5 py-1 rounded bg-purple-50 text-purple-800 border border-purple-200 text-xs font-medium">
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Section F: Interview Topics */}
                    <div className="card p-5 border border-surface-200 space-y-3">
                      <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                        <HelpCircle className="h-4 w-4 text-amber-600" />
                        <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                          Interview Topics & Systems
                        </h4>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {(preparation.interviewTopics || []).map((item, i) => (
                          <span key={i} className="px-2.5 py-1 rounded bg-amber-50 text-amber-800 border border-amber-200 text-xs font-medium">
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Section G & H: Project & Behavioral */}
                    <div className="card p-5 border border-surface-200 space-y-4">
                      {/* Project Topics */}
                      <div className="space-y-2">
                        <div className="flex items-center gap-2 border-b border-surface-100 pb-1.5">
                          <FileText className="h-4 w-4 text-emerald-600" />
                          <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                            Project Talking Points
                          </h4>
                        </div>
                        <ul className="space-y-1 text-xs text-surface-700">
                          {(preparation.projectTopics || []).map((item, i) => (
                            <li key={i} className="flex items-start gap-1.5">
                              <span className="text-emerald-500 font-bold">•</span>
                              <span>{item}</span>
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Behavioral Topics */}
                      <div className="space-y-2 pt-2 border-t border-surface-100">
                        <div className="flex items-center gap-2 border-b border-surface-100 pb-1.5">
                          <MessageSquare className="h-4 w-4 text-rose-600" />
                          <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                            Behavioral & Situational Preparation
                          </h4>
                        </div>
                        <ul className="space-y-1 text-xs text-surface-700">
                          {(preparation.behavioralTopics || []).map((item, i) => (
                            <li key={i} className="flex items-start gap-1.5">
                              <span className="text-rose-500 font-bold">•</span>
                              <span>{item}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>
                  </div>
                )}

                {/* TAB 3: Section J — Recommended Questions */}
                {activeTab === 'questions' && (
                  <div className="space-y-4">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                      <div>
                        <h3 className="text-sm font-bold text-surface-900 flex items-center gap-2">
                          <HelpCircle className="h-4 w-4 text-amber-600" />
                          Curated Role-Specific Interview Questions
                        </h3>
                        <p className="text-xs text-surface-500">
                          Click any question to inspect suggested answer guidance and core concepts.
                        </p>
                      </div>

                      <Link
                        to={`/mock-interview?targetRole=${encodeURIComponent(selectedJob.title)}`}
                        className="btn-primary btn btn-xs inline-flex items-center gap-1.5 self-start sm:self-center"
                      >
                        <Mic className="h-3 w-3" />
                        <span>Practice with AI Mock Interview</span>
                      </Link>
                    </div>

                    <div className="space-y-3">
                      {(preparation.recommendedQuestions || []).map((q, idx) => {
                        const isExpanded = expandedQuestionIdx === idx
                        return (
                          <div
                            key={idx}
                            className="card border border-surface-200 overflow-hidden transition hover:shadow-xs"
                          >
                            <button
                              onClick={() => setExpandedQuestionIdx(isExpanded ? null : idx)}
                              className="w-full p-4 text-left flex items-start justify-between gap-3 bg-white hover:bg-surface-50 transition"
                            >
                              <div className="space-y-1">
                                <div className="flex items-center gap-2">
                                  <span className={`text-[10px] font-bold px-2 py-0.5 rounded-full border ${getQuestionTypeBadge(q.type)}`}>
                                    {q.type}
                                  </span>
                                  {q.keyConcept && (
                                    <span className="text-[11px] text-surface-500 font-medium">
                                      Concept: {q.keyConcept}
                                    </span>
                                  )}
                                </div>
                                <h4 className="font-bold text-surface-900 text-sm">
                                  {idx + 1}. {q.question}
                                </h4>
                              </div>
                              {isExpanded ? (
                                <ChevronUp className="h-4 w-4 text-surface-400 flex-shrink-0 mt-1" />
                              ) : (
                                <ChevronDown className="h-4 w-4 text-surface-400 flex-shrink-0 mt-1" />
                              )}
                            </button>

                            {isExpanded && (
                              <div className="p-4 bg-surface-50 border-t border-surface-100 text-xs space-y-3 animate-slide-down">
                                <div>
                                  <span className="text-[11px] font-bold uppercase tracking-wider text-surface-500 block mb-1">
                                    Suggested Answer Guidance
                                  </span>
                                  <p className="text-surface-800 leading-relaxed font-medium bg-white p-3 rounded-lg border border-surface-200">
                                    {q.suggestedAnswer}
                                  </p>
                                </div>

                                <div className="flex items-center justify-between pt-1">
                                  <span className="text-[11px] text-surface-400">
                                    Focus: <strong>{q.keyConcept || 'Technical Clarity'}</strong>
                                  </span>
                                  <Link
                                    to={`/mock-interview?targetRole=${encodeURIComponent(selectedJob.title)}`}
                                    className="btn-secondary btn btn-xs inline-flex items-center gap-1"
                                  >
                                    <Mic className="h-3 w-3 text-rose-500" />
                                    <span>Simulate in Mock Interview</span>
                                  </Link>
                                </div>
                              </div>
                            )}
                          </div>
                        )
                      })}
                    </div>
                  </div>
                )}

                {/* TAB 4: Sections B & C — Strengths & Skill Gaps */}
                {activeTab === 'skills' && (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {/* Section B: Your Strengths */}
                    <div className="card p-5 border border-surface-200 space-y-3">
                      <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                        <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                        <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                          Your Strengths For This Role
                        </h4>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {(preparation.strengths || []).map((item, i) => (
                          <span key={i} className="px-2.5 py-1 rounded bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs font-medium">
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>

                    {/* Section C: Skill Gaps */}
                    <div className="card p-5 border border-surface-200 space-y-3">
                      <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                        <AlertCircle className="h-4 w-4 text-rose-600" />
                        <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                          Critical Skill Gaps To Bridge
                        </h4>
                      </div>
                      <div className="flex flex-wrap gap-1.5">
                        {(preparation.skillGaps || []).map((item, i) => (
                          <span key={i} className="px-2.5 py-1 rounded bg-rose-50 text-rose-800 border border-rose-200 text-xs font-medium">
                            {item}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  )
}
