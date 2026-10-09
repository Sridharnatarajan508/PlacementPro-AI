import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { careerRoadmapService, profileService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'

import {
  Map, Sparkles, Target, Clock, Layers, Zap,
  Code, MessageSquare, Flag, RefreshCw, CheckCircle2,
  BookOpen, Briefcase, Award, ArrowRight, ChevronRight, AlertCircle
} from 'lucide-react'

export default function CareerRoadmapPage() {
  const [profile, setProfile] = useState(null)
  const [roadmap, setRoadmap] = useState(null)
  const [learningRecs, setLearningRecs] = useState([])
  const [loadingInitial, setLoadingInitial] = useState(true)
  const [generating, setGenerating] = useState(false)
  const [activeTab, setActiveTab] = useState('roadmap') // 'roadmap' | 'learning' | 'projects' | 'skills'

  // Load existing roadmap
  const loadRoadmapData = useCallback(async () => {
    setLoadingInitial(true)
    try {
      // 1. Profile
      try {
        const { data: prof } = await profileService.getProfile()
        setProfile(prof)
      } catch (e) {}

      // 2. Latest Roadmap
      try {
        const { data: rm } = await careerRoadmapService.getLatestRoadmap()
        setRoadmap(rm)
      } catch (err) {
        if (err.response?.status === 404) {
          setRoadmap(null)
        } else {
          console.error('Error fetching roadmap:', err)
        }
      }

      // 3. Learning Recommendations
      try {
        const { data: recs } = await careerRoadmapService.getLearningRecommendations()
        setLearningRecs(recs || [])
      } catch (e) {}
    } finally {
      setLoadingInitial(false)
    }
  }, [])

  useEffect(() => {
    loadRoadmapData()
  }, [loadRoadmapData])

  // Generate / Regenerate
  const handleGenerate = async (forceRegenerate = false) => {
    setGenerating(true)
    try {
      const apiCall = forceRegenerate
        ? careerRoadmapService.regenerateRoadmap()
        : careerRoadmapService.generateRoadmap()

      const { data } = await apiCall
      setRoadmap(data)

      // Also refresh learning recs
      try {
        const { data: recs } = await careerRoadmapService.getLearningRecommendations()
        setLearningRecs(recs || data.learningRecommendations || [])
      } catch (e) {
        setLearningRecs(data.learningRecommendations || [])
      }

      toast.success(forceRegenerate ? 'Career Roadmap regenerated!' : 'Career Roadmap generated successfully!')
    } catch (err) {
      const status = err.response?.status
      if (status === 401) {
        toast.error('Session expired. Please log in again.')
      } else if (status === 403) {
        toast.error('You are not authorized to access this roadmap.')
      } else if (status === 404) {
        toast.error('No career roadmap found. Generate one to get started.')
      } else if (status === 429) {
        toast.error('AI service is temporarily busy. Please try again later.')
      } else if (status >= 500) {
        toast.error('Unable to generate your career roadmap right now.')
      } else {
        toast.error(getErrorMessage(err) || 'Failed to generate career roadmap.')
      }
    } finally {
      setGenerating(false)
    }
  }

  // Color helpers
  const getPriorityBadge = (priority) => {
    const p = (priority || '').toUpperCase()
    if (p === 'HIGH') {
      return 'bg-red-50 text-red-700 border-red-200'
    } else if (p === 'MEDIUM') {
      return 'bg-amber-50 text-amber-700 border-amber-200'
    }
    return 'bg-blue-50 text-blue-700 border-blue-200'
  }

  const getScoreColor = (score) => {
    if (score >= 75) return 'text-emerald-600 bg-emerald-50 border-emerald-300'
    if (score >= 50) return 'text-amber-600 bg-amber-50 border-amber-300'
    return 'text-rose-600 bg-rose-50 border-rose-300'
  }

  return (
    <AppLayout>
      <div className="space-y-6 animate-slide-up pb-12">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-surface-900 flex items-center gap-2">
              <Map className="h-6 w-6 text-primary-600" />
              AI Career Roadmap
            </h1>
            <p className="text-sm text-surface-500 mt-1">
              Your personalized AI-powered path from current skills to your target career.
            </p>
          </div>

          <div className="flex items-center gap-2">
            {roadmap ? (
              <button
                onClick={() => handleGenerate(true)}
                disabled={generating}
                className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-sm"
              >
                <RefreshCw className={`h-3.5 w-3.5 ${generating ? 'animate-spin' : ''}`} />
                <span>{generating ? 'Regenerating Roadmap...' : 'Regenerate'}</span>
              </button>
            ) : (
              <button
                onClick={() => handleGenerate(false)}
                disabled={generating}
                className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-sm"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>{generating ? 'Generating with Gemini...' : 'Generate Roadmap'}</span>
              </button>
            )}
          </div>
        </div>

        {/* Main Content Area */}
        {loadingInitial ? (
          <LoadingSpinner text="Loading your AI Career Roadmap..." />
        ) : !roadmap ? (
          <EmptyState
            icon={<Map className="text-primary-500" />}
            title="No Career Roadmap Generated Yet"
            description="Generate a personalized, step-by-step career path tailored to your verified skills, resume analysis, job market demands, and interview assessments."
            action={
              <button
                onClick={() => handleGenerate(false)}
                disabled={generating}
                className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
              >
                <Sparkles className="h-3.5 w-3.5" />
                <span>{generating ? 'Generating with Gemini...' : 'Generate Roadmap'}</span>
              </button>
            }
          />
        ) : (
          <div className="space-y-6">
            {/* 1. Career Target & Readiness Overview */}
            <div className="card p-6 border border-surface-200 bg-white shadow-xs">
              <div className="grid grid-cols-1 md:grid-cols-4 gap-6 items-center">
                {/* Readiness Score */}
                <div className="flex items-center gap-4 border-b md:border-b-0 md:border-r border-surface-100 pb-4 md:pb-0 pr-0 md:pr-4">
                  <div className={`w-20 h-20 rounded-2xl border-2 flex flex-col items-center justify-center font-bold shadow-xs ${getScoreColor(roadmap.overallReadinessScore || 0)}`}>
                    <span className="text-2xl leading-none">{roadmap.overallReadinessScore || 0}%</span>
                    <span className="text-[10px] uppercase font-bold tracking-wider mt-1 opacity-80">READINESS</span>
                  </div>
                  <div>
                    <span className="text-[11px] font-bold uppercase tracking-wider text-surface-400 block">Overall Score</span>
                    <h3 className="text-sm font-bold text-surface-900">Placement Ready</h3>
                    <span className="text-xs text-surface-500">
                      {roadmap.overallReadinessScore >= 75 ? 'Strong Baseline' : 'Growth In Progress'}
                    </span>
                  </div>
                </div>

                {/* Target Role & Level */}
                <div className="md:col-span-2 space-y-2">
                  <div className="flex items-center gap-2 flex-wrap">
                    <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-primary-50 text-primary-700 border border-primary-200">
                      Target: {roadmap.targetRole}
                    </span>
                    <span className="text-xs font-medium px-2.5 py-0.5 rounded-full bg-surface-100 text-surface-600">
                      Level: {roadmap.currentLevel || 'Fresher / Entry Level'}
                    </span>
                  </div>
                  {/* 2. AI Career Summary */}
                  <p className="text-xs text-surface-600 leading-relaxed">
                    {roadmap.summary || 'Your customized career roadmap designed by Gemini AI.'}
                  </p>
                </div>

                {/* Quick Action */}
                <div className="flex flex-col items-start md:items-end justify-center">
                  <span className="text-[11px] text-surface-400 mb-1">
                    {roadmap.roadmap?.length || 0} Learning Phases
                  </span>
                  <button
                    onClick={() => handleGenerate(true)}
                    disabled={generating}
                    className="btn-secondary btn btn-xs inline-flex items-center gap-1"
                  >
                    <RefreshCw className={`h-3 w-3 ${generating ? 'animate-spin' : ''}`} />
                    <span>Regenerate AI Plan</span>
                  </button>
                </div>
              </div>
            </div>

            {/* Navigation Tabs */}
            <div className="flex items-center gap-2 border-b border-surface-200 pb-1 flex-wrap">
              <button
                onClick={() => setActiveTab('roadmap')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeTab === 'roadmap'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <Layers className="h-3.5 w-3.5 text-primary-600" />
                <span>Learning Roadmap ({roadmap.roadmap?.length || 0} Phases)</span>
              </button>

              <button
                onClick={() => setActiveTab('learning')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeTab === 'learning'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <BookOpen className="h-3.5 w-3.5 text-amber-600" />
                <span>Learning Recommendations ({learningRecs.length || roadmap.learningRecommendations?.length || 0})</span>
              </button>

              <button
                onClick={() => setActiveTab('projects')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeTab === 'projects'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <Code className="h-3.5 w-3.5 text-purple-600" />
                <span>Recommended Projects ({roadmap.projectRecommendations?.length || 0})</span>
              </button>

              <button
                onClick={() => setActiveTab('skills')}
                className={`px-4 py-2 text-xs font-bold rounded-lg transition flex items-center gap-1.5 ${
                  activeTab === 'skills'
                    ? 'bg-primary-50 text-primary-700 border border-primary-200 shadow-xs'
                    : 'text-surface-600 hover:text-surface-900'
                }`}
              >
                <Zap className="h-3.5 w-3.5 text-emerald-600" />
                <span>Skills Breakdown</span>
              </button>
            </div>

            {/* TAB 1: 6. Learning Roadmap Phases */}
            {activeTab === 'roadmap' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-surface-900 flex items-center gap-2">
                    <Layers className="h-4 w-4 text-primary-600" />
                    Curated Step-by-Step Learning Timeline
                  </h3>
                  <span className="text-xs text-surface-400">Chronological Phase Plan</span>
                </div>

                <div className="grid grid-cols-1 gap-4">
                  {(roadmap.roadmap || []).map((phase, idx) => (
                    <div key={idx} className="card p-5 border border-surface-200 hover:shadow-md transition space-y-4">
                      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2 border-b border-surface-100 pb-3">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-xl bg-primary-100 text-primary-800 font-bold text-xs flex items-center justify-center flex-shrink-0">
                            {phase.phase || idx + 1}
                          </div>
                          <div>
                            <h4 className="font-bold text-surface-900 text-sm">{phase.title}</h4>
                            <p className="text-xs text-surface-500 mt-0.5">{phase.goal}</p>
                          </div>
                        </div>
                        <span className="badge-primary text-[11px] px-2.5 py-0.5 rounded-full self-start sm:self-center">
                          Phase {phase.phase || idx + 1}
                        </span>
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
                        {/* Skills */}
                        <div className="space-y-1.5">
                          <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                            Key Skills
                          </span>
                          <div className="flex flex-wrap gap-1">
                            {(phase.skills || []).map((s, i) => (
                              <span key={i} className="px-2 py-0.5 rounded bg-primary-50 text-primary-700 border border-primary-100 font-medium">
                                {s}
                              </span>
                            ))}
                          </div>
                        </div>

                        {/* Topics */}
                        <div className="space-y-1.5">
                          <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                            Core Topics
                          </span>
                          <ul className="space-y-1">
                            {(phase.topics || []).map((t, i) => (
                              <li key={i} className="flex items-start gap-1.5 text-surface-600">
                                <ChevronRight className="h-3 w-3 text-primary-500 mt-0.5 flex-shrink-0" />
                                <span>{t}</span>
                              </li>
                            ))}
                          </ul>
                        </div>

                        {/* Projects / Milestone */}
                        <div className="space-y-1.5">
                          <span className="text-[11px] font-bold text-surface-500 uppercase tracking-wider block">
                            Milestone Project
                          </span>
                          <div className="bg-surface-50 p-2.5 rounded-lg border border-surface-200">
                            {(phase.projects || []).map((p, i) => (
                              <p key={i} className="text-surface-700 font-medium">
                                🚀 {p}
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

            {/* TAB 2: 8. Learning Recommendations */}
            {activeTab === 'learning' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-surface-900 flex items-center gap-2">
                    <BookOpen className="h-4 w-4 text-amber-600" />
                    Personalized Skill Learning Objectives & Guidance
                  </h3>
                  <span className="text-xs text-surface-400">Prioritized Technical Action Items</span>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {(learningRecs.length > 0 ? learningRecs : roadmap.learningRecommendations || []).map((rec, idx) => (
                    <div key={idx} className="card p-5 border border-surface-200 space-y-3 hover:shadow-md transition">
                      <div className="flex items-center justify-between gap-2">
                        <h4 className="font-bold text-surface-900 text-sm flex items-center gap-2">
                          <Zap className="h-4 w-4 text-amber-500" />
                          {rec.skill}
                        </h4>
                        <span className={`text-[11px] font-bold px-2 py-0.5 rounded-full border ${getPriorityBadge(rec.priority)}`}>
                          {rec.priority} PRIORITY
                        </span>
                      </div>

                      <div className="space-y-2 text-xs">
                        <div>
                          <span className="text-surface-400 block font-medium">Placement Importance:</span>
                          <p className="text-surface-700 mt-0.5">{rec.reason}</p>
                        </div>

                        <div>
                          <span className="text-surface-400 block font-medium">Learning Goal:</span>
                          <p className="text-surface-700 mt-0.5 font-medium">{rec.learningObjective}</p>
                        </div>

                        {rec.recommendedTopics && rec.recommendedTopics.length > 0 && (
                          <div>
                            <span className="text-surface-400 block font-medium mb-1">Recommended Topics:</span>
                            <div className="flex flex-wrap gap-1">
                              {rec.recommendedTopics.map((top, i) => (
                                <span key={i} className="px-2 py-0.5 rounded bg-surface-100 text-surface-700 text-[11px]">
                                  {top}
                                </span>
                              ))}
                            </div>
                          </div>
                        )}

                        {rec.projectIdea && (
                          <div className="bg-amber-50/50 border border-amber-100 rounded-lg p-2.5">
                            <span className="text-[10px] font-bold uppercase tracking-wider text-amber-700 block">
                              Hands-on Project Idea
                            </span>
                            <p className="text-surface-800 font-medium mt-0.5">
                              {rec.projectIdea}
                            </p>
                          </div>
                        )}

                        {rec.estimatedEffort && (
                          <div className="flex items-center gap-1.5 text-surface-500 pt-1">
                            <Clock className="h-3.5 w-3.5 text-surface-400" />
                            <span>Estimated Effort: <strong className="text-surface-700">{rec.estimatedEffort}</strong></span>
                          </div>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 3: 7. Recommended Projects */}
            {activeTab === 'projects' && (
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-sm font-bold text-surface-900 flex items-center gap-2">
                    <Code className="h-4 w-4 text-purple-600" />
                    Placement-Ready Capstone & Portfolio Projects
                  </h3>
                  <span className="text-xs text-surface-400">High-Impact Resume Additions</span>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {(roadmap.projectRecommendations || []).map((proj, idx) => (
                    <div key={idx} className="card p-5 border border-surface-200 hover:shadow-md transition space-y-3">
                      <div className="flex items-start gap-3">
                        <div className="w-8 h-8 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center font-bold text-xs flex-shrink-0">
                          #{idx + 1}
                        </div>
                        <div className="space-y-1">
                          <h4 className="font-bold text-surface-900 text-sm leading-snug">{proj}</h4>
                          <p className="text-xs text-surface-500">
                            Build, test, and deploy this project to showcase hands-on mastery in your technical interviews.
                          </p>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* TAB 4: 3, 4, 5. Skills Breakdown (Current, Gaps, Recommended) */}
            {activeTab === 'skills' && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* 3. Current Skills */}
                <div className="card p-5 border border-surface-200 space-y-3">
                  <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                    <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                    <h4 className="font-bold text-surface-900 text-xs">Current Verified Skills</h4>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {(roadmap.currentSkills || []).length > 0 ? (
                      roadmap.currentSkills.map((s, i) => (
                        <span key={i} className="px-2.5 py-1 rounded-md bg-emerald-50 text-emerald-800 border border-emerald-200 text-xs font-medium">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-surface-400">None detected</span>
                    )}
                  </div>
                </div>

                {/* 4. Skill Gaps */}
                <div className="card p-5 border border-surface-200 space-y-3">
                  <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                    <AlertCircle className="h-4 w-4 text-rose-600" />
                    <h4 className="font-bold text-surface-900 text-xs">Identified Skill Gaps</h4>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {(roadmap.skillGaps || []).length > 0 ? (
                      roadmap.skillGaps.map((s, i) => (
                        <span key={i} className="px-2.5 py-1 rounded-md bg-rose-50 text-rose-800 border border-rose-200 text-xs font-medium">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-surface-400">No major gaps</span>
                    )}
                  </div>
                </div>

                {/* 5. Recommended Skills */}
                <div className="card p-5 border border-surface-200 space-y-3">
                  <div className="flex items-center gap-2 border-b border-surface-100 pb-2">
                    <Zap className="h-4 w-4 text-primary-600" />
                    <h4 className="font-bold text-surface-900 text-xs">Recommended Technologies</h4>
                  </div>
                  <div className="flex flex-wrap gap-1.5">
                    {(roadmap.recommendedSkills || []).length > 0 ? (
                      roadmap.recommendedSkills.map((s, i) => (
                        <span key={i} className="px-2.5 py-1 rounded-md bg-primary-50 text-primary-800 border border-primary-200 text-xs font-medium">
                          {s}
                        </span>
                      ))
                    ) : (
                      <span className="text-xs text-surface-400">None</span>
                    )}
                  </div>
                </div>
              </div>
            )}

            {/* 9. Job Readiness Summary Section */}
            {roadmap.jobReadinessSummary && (
              <div className="card p-5 border border-emerald-200 bg-emerald-50/30 rounded-xl space-y-2">
                <div className="flex items-center gap-2">
                  <Award className="h-4 w-4 text-emerald-600" />
                  <h4 className="font-bold text-surface-900 text-xs uppercase tracking-wider">
                    AI Job Readiness & Placement Advisory
                  </h4>
                </div>
                <p className="text-xs text-surface-700 leading-relaxed font-medium">
                  {roadmap.jobReadinessSummary}
                </p>
              </div>
            )}
          </div>
        )}
      </div>
    </AppLayout>
  )
}
