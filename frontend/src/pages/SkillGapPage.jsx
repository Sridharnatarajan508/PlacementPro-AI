import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { skillGapService, jobService, profileService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import {
  Zap, Sparkles, Target, Briefcase, CheckCircle2, AlertTriangle,
  Clock, BookOpen, Layers, ArrowRight, RefreshCw, Trash2,
  Calendar, Award, Code, CheckCircle, ChevronRight
} from 'lucide-react'

export default function SkillGapPage() {
  const [targetRole, setTargetRole] = useState('')
  const [selectedJobId, setSelectedJobId] = useState('')
  const [availableJobs, setAvailableJobs] = useState([])
  const [profile, setProfile] = useState(null)

  const [currentAnalysis, setCurrentAnalysis] = useState(null)
  const [history, setHistory] = useState([])

  const [loadingInitial, setLoadingInitial] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [activeViewTab, setActiveViewTab] = useState('gaps') // 'gaps' | 'roadmap' | 'skills'

  // Load initial data (profile, latest analysis, jobs list, history)
  const loadInitialData = useCallback(async () => {
    setLoadingInitial(true)
    try {
      // 1. Fetch Profile
      try {
        const { data: prof } = await profileService.getProfile()
        setProfile(prof)
        if (prof?.targetRole) {
          setTargetRole(prof.targetRole)
        }
      } catch (e) {
        // Profile might be empty
      }

      // 2. Fetch Available Jobs for optional selection
      try {
        const { data: jobs } = await jobService.getJobs()
        setAvailableJobs(jobs || [])
      } catch (e) {
        // Jobs list non-critical
      }

      // 3. Fetch Latest Skill Gap Analysis
      try {
        const { data: latest } = await skillGapService.getLatestSkillGap()
        if (latest) {
          setCurrentAnalysis(latest)
          if (latest.targetRole) setTargetRole(latest.targetRole)
          if (latest.jobId) setSelectedJobId(String(latest.jobId))
        }
      } catch (e) {
        // No analysis yet
      }

      // 4. Fetch History
      try {
        const { data: hist } = await skillGapService.getSkillGapHistory()
        setHistory(hist || [])
      } catch (e) {
        // History non-critical
      }
    } finally {
      setLoadingInitial(false)
    }
  }, [])

  useEffect(() => {
    loadInitialData()
  }, [loadInitialData])

  // When a job is selected from dropdown, update targetRole
  const handleJobSelect = (e) => {
    const jId = e.target.value
    setSelectedJobId(jId)
    if (jId) {
      const found = availableJobs.find(j => String(j.id) === String(jId))
      if (found) {
        setTargetRole(found.title)
      }
    }
  }

  // Trigger Analysis
  const handleAnalyze = async (forceRefresh = false) => {
    if (!targetRole.trim() && !selectedJobId) {
      toast.error('Please enter a target role or choose a job opening.')
      return
    }

    setAnalyzing(true)
    try {
      const payload = {
        targetRole: targetRole.trim() || undefined,
        jobId: selectedJobId ? Number(selectedJobId) : undefined,
        forceRefresh,
      }

      const { data } = await skillGapService.analyzeSkillGap(payload)
      setCurrentAnalysis(data)
      toast.success(
        forceRefresh
          ? 'Skill gap re-analyzed with fresh AI recommendations!'
          : 'Skill gap analysis ready!'
      )

      // Refresh history list
      try {
        const { data: hist } = await skillGapService.getSkillGapHistory()
        setHistory(hist || [])
      } catch (e) {}
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setAnalyzing(false)
    }
  }

  // Delete Analysis
  const handleDeleteAnalysis = async (id) => {
    try {
      await skillGapService.deleteSkillGap(id)
      toast.success('Analysis deleted')
      if (currentAnalysis?.id === id) {
        setCurrentAnalysis(null)
      }
      setHistory(prev => prev.filter(h => h.id !== id))
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  // Select historical analysis
  const handleSelectHistory = (analysis) => {
    setCurrentAnalysis(analysis)
    if (analysis.targetRole) setTargetRole(analysis.targetRole)
    if (analysis.jobId) setSelectedJobId(String(analysis.jobId))
  }

  // Helpers for badge styling
  const getReadinessTheme = (score) => {
    if (score >= 80) return { label: 'High Readiness', badge: 'bg-emerald-100 text-emerald-800 border-emerald-300', text: 'text-emerald-600', ring: 'ring-emerald-400' }
    if (score >= 65) return { label: 'Good Readiness', badge: 'bg-blue-100 text-blue-800 border-blue-300', text: 'text-blue-600', ring: 'ring-blue-400' }
    if (score >= 50) return { label: 'Moderate Readiness', badge: 'bg-amber-100 text-amber-800 border-amber-300', text: 'text-amber-600', ring: 'ring-amber-400' }
    return { label: 'Needs Preparation', badge: 'bg-rose-100 text-rose-800 border-rose-300', text: 'text-rose-600', ring: 'ring-rose-400' }
  }

  const getPriorityTheme = (p) => {
    switch (p?.toUpperCase()) {
      case 'HIGH': return 'bg-rose-100 text-rose-800 border-rose-200'
      case 'MEDIUM': return 'bg-amber-100 text-amber-800 border-amber-200'
      case 'LOW': return 'bg-blue-100 text-blue-800 border-blue-200'
      default: return 'bg-surface-100 text-surface-700 border-surface-200'
    }
  }

  const totalLearningHours = currentAnalysis?.skillGaps?.reduce(
    (acc, curr) => acc + (curr.estimatedLearningHours || 0), 0
  ) || 0

  return (
    <AppLayout>
      <div className="space-y-6 animate-slide-up pb-12">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-surface-900 flex items-center gap-2">
              <Zap className="h-6 w-6 text-primary-600" />
              AI Skill Gap Analyzer
            </h1>
            <p className="text-sm text-surface-500 mt-1">
              Discover the critical skills you need for your target role and get a personalized, step-by-step learning plan.
            </p>
          </div>

          {/* History selector if available */}
          {history.length > 1 && (
            <div className="flex items-center gap-2">
              <span className="text-xs text-surface-500 font-medium">History:</span>
              <select
                value={currentAnalysis?.id || ''}
                onChange={(e) => {
                  const found = history.find(h => String(h.id) === e.target.value)
                  if (found) handleSelectHistory(found)
                }}
                className="input py-1 text-xs max-w-[200px]"
              >
                {history.map((h) => (
                  <option key={h.id} value={h.id}>
                    {h.targetRole} ({h.readinessScore}%)
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Target Configuration Card */}
        <div className="card p-5 border border-surface-200 shadow-sm space-y-4">
          <div className="flex items-center gap-2 text-surface-800 font-semibold text-sm">
            <Target className="h-4 w-4 text-primary-600" />
            <span>Target Role & Position Configuration</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Target Role Input */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-surface-700">Target Role Title</label>
              <input
                type="text"
                placeholder="e.g. Java Backend Developer, Full Stack Engineer..."
                value={targetRole}
                onChange={(e) => {
                  setTargetRole(e.target.value)
                  setSelectedJobId('')
                }}
                className="input text-xs w-full"
              />
              <p className="text-[11px] text-surface-400">
                Specify your desired job title or role in tech.
              </p>
            </div>

            {/* Optional Job Opening Dropdown */}
            <div className="space-y-1">
              <label className="text-xs font-semibold text-surface-700">
                Match Against Specific Job (Optional)
              </label>
              <select
                value={selectedJobId}
                onChange={handleJobSelect}
                className="input text-xs w-full"
              >
                <option value="">-- General Target Role (No specific job) --</option>
                {availableJobs.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.title} @ {j.company} ({j.location})
                  </option>
                ))}
              </select>
              <p className="text-[11px] text-surface-400">
                Optionally compare against real vacancies in the database.
              </p>
            </div>
          </div>

          {/* Action Row */}
          <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-surface-100">
            <div className="text-xs text-surface-500">
              {currentAnalysis ? (
                <span className="flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-surface-400" />
                  Last analyzed on {new Date(currentAnalysis.analyzedAt || currentAnalysis.createdAt).toLocaleDateString()}
                </span>
              ) : (
                <span>AI will benchmark your profile against industry standards.</span>
              )}
            </div>

            <div className="flex items-center gap-2">
              {currentAnalysis && (
                <button
                  onClick={() => handleAnalyze(true)}
                  disabled={analyzing}
                  className="btn-secondary btn btn-sm inline-flex items-center gap-1.5"
                  title="Run fresh AI analysis"
                >
                  <RefreshCw className={`h-3.5 w-3.5 ${analyzing ? 'animate-spin' : ''}`} />
                  <span>Analyze Again</span>
                </button>
              )}

              <button
                onClick={() => handleAnalyze(false)}
                disabled={analyzing}
                className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>{analyzing ? 'Analyzing with Gemini...' : currentAnalysis ? 'Update Analysis' : 'Analyze My Skill Gap'}</span>
              </button>
            </div>
          </div>
        </div>

        {/* Main Content Area */}
        {loadingInitial ? (
          <LoadingSpinner text="Loading skill gap analysis..." />
        ) : !currentAnalysis ? (
          <EmptyState
            icon={<Zap className="text-primary-500" />}
            title="No Skill Gap Analysis Yet"
            description="Enter your target role or select a job vacancy above to calculate your readiness score, identify missing skills, and generate a personalized learning roadmap."
            action={
              <button onClick={() => handleAnalyze(false)} className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                <Sparkles className="h-3.5 w-3.5" />
                Analyze My Skills Now
              </button>
            }
          />
        ) : (
          <div className="space-y-6">
            {/* Top Score Banner */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
              {/* Readiness Card */}
              <div className="card p-5 bg-white border border-surface-200 shadow-sm flex items-center gap-4">
                <div className={`w-16 h-16 rounded-2xl flex flex-col items-center justify-center font-bold border-2 ${getReadinessTheme(currentAnalysis.readinessScore).badge}`}>
                  <span className="text-2xl leading-none">{currentAnalysis.readinessScore}%</span>
                  <span className="text-[10px] font-medium opacity-80 mt-0.5">READINESS</span>
                </div>
                <div className="space-y-1">
                  <span className={`inline-block px-2 py-0.5 rounded-full text-xs font-bold ${getReadinessTheme(currentAnalysis.readinessScore).badge}`}>
                    {getReadinessTheme(currentAnalysis.readinessScore).label}
                  </span>
                  <h3 className="font-bold text-surface-900 text-sm">Role Compatibility</h3>
                  <p className="text-xs text-surface-400">Target: {currentAnalysis.targetRole}</p>
                </div>
              </div>

              {/* Skill Coverage Card */}
              <div className="card p-5 bg-white border border-surface-200 shadow-sm flex flex-col justify-between space-y-2">
                <div className="flex items-center justify-between text-xs font-medium">
                  <span className="flex items-center gap-1.5 text-surface-600">
                    <Award className="h-4 w-4 text-blue-500" />
                    Essential Skills Coverage
                  </span>
                  <span className="font-bold text-surface-900 text-sm">{currentAnalysis.skillCoverageScore}%</span>
                </div>
                <div className="h-3 w-full bg-surface-100 rounded-full overflow-hidden">
                  <div
                    className="h-full bg-blue-500 rounded-full transition-all duration-500"
                    style={{ width: `${currentAnalysis.skillCoverageScore}%` }}
                  />
                </div>
                <p className="text-[11px] text-surface-400">
                  {currentAnalysis.currentSkills?.length || 0} skills verified • {currentAnalysis.missingSkills?.length || 0} gaps detected
                </p>
              </div>

              {/* Learning Investment Card */}
              <div className="card p-5 bg-white border border-surface-200 shadow-sm flex items-center justify-between">
                <div className="space-y-1">
                  <span className="text-xs font-medium text-surface-500 flex items-center gap-1">
                    <Clock className="h-3.5 w-3.5 text-purple-500" />
                    Estimated Learning Time
                  </span>
                  <p className="text-2xl font-bold text-surface-900">
                    ~{totalLearningHours} <span className="text-sm font-normal text-surface-500">hours</span>
                  </p>
                  <p className="text-xs text-surface-400">
                    Across {currentAnalysis.learningPlan?.length || 0} personalized phases
                  </p>
                </div>
                <div className="p-3 bg-purple-50 rounded-xl text-purple-600">
                  <BookOpen className="h-6 w-6" />
                </div>
              </div>
            </div>

            {/* AI Summary Banner */}
            {currentAnalysis.summary && (
              <div className="bg-primary-50/70 border border-primary-100 rounded-xl p-5 shadow-2xs">
                <h3 className="text-xs font-bold text-primary-900 uppercase tracking-wider mb-1.5 flex items-center gap-1.5">
                  <Sparkles className="h-4 w-4 text-primary-600" />
                  AI Executive Gap Evaluation
                </h3>
                <p className="text-xs text-primary-950 leading-relaxed">
                  {currentAnalysis.summary}
                </p>
              </div>
            )}

            {/* View Tabs */}
            <div className="flex items-center gap-2 border-b border-surface-200 pb-1">
              <button
                onClick={() => setActiveViewTab('gaps')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeViewTab === 'gaps'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
                <span>Priority Skill Gaps ({currentAnalysis.skillGaps?.length || 0})</span>
              </button>

              <button
                onClick={() => setActiveViewTab('roadmap')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeViewTab === 'roadmap'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <Layers className="h-3.5 w-3.5 text-primary-600" />
                <span>Personalized Learning Roadmap ({currentAnalysis.learningPlan?.length || 0} Phases)</span>
              </button>

              <button
                onClick={() => setActiveViewTab('skills')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeViewTab === 'skills'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <CheckCircle2 className="h-3.5 w-3.5 text-emerald-500" />
                <span>Skills Matrix & Inventory</span>
              </button>
            </div>

            {/* TAB 1: Skill Gaps Detailed Cards */}
            {activeViewTab === 'gaps' && (
              <div className="space-y-4">
                {currentAnalysis.skillGaps?.length === 0 ? (
                  <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-6 text-center">
                    <CheckCircle className="h-8 w-8 text-emerald-600 mx-auto mb-2" />
                    <h3 className="font-bold text-emerald-900 text-sm">No Major Skill Gaps Detected!</h3>
                    <p className="text-xs text-emerald-700 mt-1">Your existing skills comprehensively cover this target role.</p>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {currentAnalysis.skillGaps.map((gap, index) => (
                      <div key={index} className="card p-5 border border-surface-200 hover:shadow-md transition flex flex-col justify-between space-y-3">
                        {/* Card Header */}
                        <div>
                          <div className="flex items-start justify-between gap-2">
                            <div className="space-y-1">
                              <div className="flex items-center gap-1.5 flex-wrap">
                                <span className={`text-[11px] font-bold px-2 py-0.5 rounded-md border ${getPriorityTheme(gap.priority)}`}>
                                  {gap.priority} PRIORITY
                                </span>
                                {gap.importance && (
                                  <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-surface-100 text-surface-700">
                                    {gap.importance} Importance
                                  </span>
                                )}
                                {gap.difficulty && (
                                  <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-purple-50 text-purple-700">
                                    {gap.difficulty}
                                  </span>
                                )}
                              </div>
                              <h3 className="font-bold text-surface-900 text-base mt-1">
                                {gap.skillName}
                              </h3>
                            </div>

                            {gap.estimatedLearningHours && (
                              <span className="text-xs font-semibold text-surface-500 bg-surface-50 border border-surface-200 px-2 py-1 rounded-lg flex items-center gap-1 flex-shrink-0">
                                <Clock className="h-3 w-3 text-purple-500" />
                                {gap.estimatedLearningHours}h
                              </span>
                            )}
                          </div>

                          {/* Level comparison */}
                          <div className="flex items-center gap-3 text-xs text-surface-600 bg-surface-50 p-2.5 rounded-lg border border-surface-100 mt-3">
                            <div>
                              <span className="text-surface-400 block text-[10px] uppercase">Current Level</span>
                              <span className="font-semibold text-surface-700">{gap.currentLevel || 'None'}</span>
                            </div>
                            <ChevronRight className="h-4 w-4 text-surface-300" />
                            <div>
                              <span className="text-surface-400 block text-[10px] uppercase">Target Level</span>
                              <span className="font-semibold text-primary-700">{gap.requiredLevel || 'Intermediate'}</span>
                            </div>
                          </div>

                          {/* Reason */}
                          {gap.reason && (
                            <p className="text-xs text-surface-600 leading-relaxed mt-3">
                              <span className="font-semibold text-surface-700">Why it matters: </span>
                              {gap.reason}
                            </p>
                          )}
                        </div>

                        {/* Technologies & Projects */}
                        <div className="space-y-2 pt-2 border-t border-surface-100">
                          {gap.recommendedTechnologies?.length > 0 && (
                            <div className="space-y-1">
                              <span className="text-[11px] font-semibold text-surface-500">Recommended Tech:</span>
                              <div className="flex flex-wrap gap-1">
                                {gap.recommendedTechnologies.map((tech, i) => (
                                  <span key={i} className="bg-primary-50 text-primary-700 text-[11px] px-2 py-0.5 rounded font-medium border border-primary-100">
                                    {tech}
                                  </span>
                                ))}
                              </div>
                            </div>
                          )}

                          {gap.practiceProjects?.length > 0 && (
                            <div className="space-y-1">
                              <span className="text-[11px] font-semibold text-surface-500">Practice Project:</span>
                              <p className="text-xs text-surface-700 italic bg-surface-50 p-2 rounded border border-surface-100">
                                💡 {gap.practiceProjects[0]}
                              </p>
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 2: Personalized Learning Roadmap */}
            {activeViewTab === 'roadmap' && (
              <div className="space-y-4">
                {currentAnalysis.learningPlan?.length === 0 ? (
                  <div className="bg-surface-50 border border-surface-200 rounded-xl p-6 text-center">
                    <BookOpen className="h-8 w-8 text-surface-400 mx-auto mb-2" />
                    <p className="text-xs text-surface-500">No learning plan steps generated.</p>
                  </div>
                ) : (
                  <div className="relative pl-6 border-l-2 border-primary-200 space-y-6">
                    {currentAnalysis.learningPlan.map((step, idx) => (
                      <div key={idx} className="relative">
                        {/* Step Marker */}
                        <div className="absolute -left-[31px] top-1.5 w-6 h-6 rounded-full bg-primary-600 text-white text-xs font-bold flex items-center justify-center ring-4 ring-white shadow-xs">
                          {step.step || idx + 1}
                        </div>

                        {/* Step Card */}
                        <div className="card p-5 border border-surface-200 space-y-3">
                          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
                            <div>
                              <span className="text-[11px] font-bold uppercase tracking-wider text-primary-600">
                                {step.phaseTitle || `Phase ${idx + 1}`}
                              </span>
                              <h3 className="font-bold text-surface-900 text-base mt-0.5">
                                {step.skillName}
                              </h3>
                            </div>

                            {step.estimatedHours && (
                              <span className="text-xs font-semibold text-purple-700 bg-purple-50 border border-purple-200 px-2.5 py-1 rounded-lg flex items-center gap-1 self-start sm:self-auto">
                                <Clock className="h-3 w-3" />
                                {step.estimatedHours} Hours
                              </span>
                            )}
                          </div>

                          <p className="text-xs text-surface-600 leading-relaxed">
                            {step.description}
                          </p>

                          {/* Key Topics */}
                          {step.keyTopics?.length > 0 && (
                            <div className="space-y-1 pt-2 border-t border-surface-100">
                              <span className="text-[11px] font-semibold text-surface-500">Key Subtopics to Master:</span>
                              <div className="flex flex-wrap gap-1.5">
                                {step.keyTopics.map((topic, i) => (
                                  <span key={i} className="badge-surface text-[11px] px-2 py-0.5 rounded font-medium">
                                    ✓ {topic}
                                  </span>
                                ))}
                              </div>
                            </div>
                          )}

                          {/* Milestone Project */}
                          {step.milestoneProject && (
                            <div className="bg-emerald-50/70 border border-emerald-200 rounded-lg p-3 text-xs text-emerald-900 mt-2">
                              <span className="font-bold block text-[10px] uppercase text-emerald-700 tracking-wider mb-0.5">
                                🚀 Milestone Capstone Project
                              </span>
                              {step.milestoneProject}
                            </div>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* TAB 3: Skills Inventory Matrix */}
            {activeViewTab === 'skills' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                {/* Verified Current Skills */}
                <div className="card p-5 border border-emerald-200 bg-emerald-50/30 space-y-3">
                  <div className="flex items-center gap-1.5 text-emerald-800 text-xs font-bold uppercase tracking-wider">
                    <CheckCircle className="h-4 w-4 text-emerald-600" />
                    <span>Current Verified Skills ({currentAnalysis.currentSkills?.length || 0})</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {currentAnalysis.currentSkills?.map((s, i) => (
                      <span key={i} className="bg-white border border-emerald-300 text-emerald-800 text-xs px-2.5 py-1 rounded-md font-medium shadow-2xs">
                        ✓ {s}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Required Role Skills */}
                <div className="card p-5 border border-blue-200 bg-blue-50/30 space-y-3">
                  <div className="flex items-center gap-1.5 text-blue-800 text-xs font-bold uppercase tracking-wider">
                    <Award className="h-4 w-4 text-blue-600" />
                    <span>Required Role Skills ({currentAnalysis.requiredSkills?.length || 0})</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {currentAnalysis.requiredSkills?.map((s, i) => (
                      <span key={i} className="bg-white border border-blue-300 text-blue-800 text-xs px-2.5 py-1 rounded-md font-medium shadow-2xs">
                        {s}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Missing Skills */}
                <div className="card p-5 border border-rose-200 bg-rose-50/30 space-y-3">
                  <div className="flex items-center gap-1.5 text-rose-800 text-xs font-bold uppercase tracking-wider">
                    <AlertTriangle className="h-4 w-4 text-rose-600" />
                    <span>Identified Gaps ({currentAnalysis.missingSkills?.length || 0})</span>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {currentAnalysis.missingSkills?.map((s, i) => (
                      <span key={i} className="bg-white border border-rose-300 text-rose-800 text-xs px-2.5 py-1 rounded-md font-medium shadow-2xs">
                        ✗ {s}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  )
}
