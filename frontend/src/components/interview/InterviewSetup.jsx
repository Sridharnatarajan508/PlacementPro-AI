import { useState, useEffect } from 'react'
import {
  Mic, Sparkles, Target, Briefcase, Award,
  CheckCircle, HelpCircle, Layers, Sliders, Play
} from 'lucide-react'
import { jobService } from '../../services/api'

const INTERVIEW_TYPES = [
  { id: 'MIXED', label: 'Mixed (Recommended)', desc: 'Holistic combination of technical, system design, and behavioral questions.' },
  { id: 'TECHNICAL', label: 'Technical Only', desc: 'Core programming languages, frameworks, SQL queries, algorithms, and design.' },
  { id: 'BEHAVIORAL', label: 'Behavioral & Leadership', desc: 'STAR methodology questions evaluating teamwork, conflict, and problem ownership.' },
  { id: 'HR', label: 'HR & Culture Fit', desc: 'Career goals, work ethic, culture alignment, and communication style.' },
]

const DIFFICULTIES = [
  { id: 'EASY', label: 'Junior / Entry', desc: 'Fundamental concepts & standard questions.' },
  { id: 'MEDIUM', label: 'Mid-Level', desc: 'Scenario-based, practical trade-offs & edge cases.' },
  { id: 'HARD', label: 'Senior / Lead', desc: 'Complex distributed architecture, scalability & deep internals.' },
]

const QUESTION_COUNTS = [
  { count: 5, label: '5 Questions', time: '~10 min' },
  { count: 10, label: '10 Questions', time: '~25 min' },
  { count: 15, label: '15 Questions', time: '~40 min' },
]

export default function InterviewSetup({ initialRole = '', onStart, starting = false }) {
  const [targetRole, setTargetRole] = useState(initialRole || '')
  const [targetJobId, setTargetJobId] = useState('')
  const [interviewType, setInterviewType] = useState('MIXED')
  const [difficulty, setDifficulty] = useState('MEDIUM')
  const [totalQuestions, setTotalQuestions] = useState(10)
  const [jobs, setJobs] = useState([])

  useEffect(() => {
    if (initialRole && !targetRole) {
      setTargetRole(initialRole)
    }
  }, [initialRole])

  useEffect(() => {
    jobService.getJobs({ page: 0, size: 20 })
      .then(({ data }) => {
        setJobs(data.content || data || [])
      })
      .catch(() => {})
  }, [])

  const handleJobSelect = (e) => {
    const jId = e.target.value
    setTargetJobId(jId)
    if (jId) {
      const selected = jobs.find(j => j.id.toString() === jId.toString())
      if (selected && !targetRole) {
        setTargetRole(selected.title)
      }
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    onStart({
      targetRole: targetRole.trim() || 'Software Engineer',
      targetJobId: targetJobId ? Number(targetJobId) : null,
      interviewType,
      difficulty,
      totalQuestions: Number(totalQuestions),
    })
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div className="card p-6 border border-surface-200 shadow-sm space-y-6">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Mic className="h-5 w-5 text-primary-600" />
            <h2 className="text-xl font-bold text-surface-900">Configure AI Mock Interview</h2>
          </div>
          <p className="text-xs text-surface-500">
            Practice with an intelligent AI interviewer tailored to your target position, resume skillset, and career background.
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Target Role & Job Selection */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-surface-700 flex items-center gap-1">
                <Target className="h-3.5 w-3.5 text-primary-600" />
                Target Job Role
              </label>
              <input
                type="text"
                placeholder="e.g. Java Backend Engineer, Full Stack React Developer"
                value={targetRole}
                onChange={(e) => setTargetRole(e.target.value)}
                className="input text-xs w-full"
                required
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-semibold text-surface-700 flex items-center gap-1">
                <Briefcase className="h-3.5 w-3.5 text-purple-600" />
                Target Job Vacancy (Optional)
              </label>
              <select
                value={targetJobId}
                onChange={handleJobSelect}
                className="input text-xs w-full"
              >
                <option value="">-- General Role Interview --</option>
                {jobs.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.title} @ {j.company} ({j.location})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Interview Type */}
          <div className="space-y-2">
            <label className="text-xs font-semibold text-surface-700 flex items-center gap-1">
              <Layers className="h-3.5 w-3.5 text-blue-600" />
              Interview Type
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {INTERVIEW_TYPES.map((t) => {
                const active = interviewType === t.id
                return (
                  <button
                    type="button"
                    key={t.id}
                    onClick={() => setInterviewType(t.id)}
                    className={`p-3 rounded-xl border text-left transition ${
                      active
                        ? 'border-primary-500 bg-primary-50/40 shadow-xs ring-1 ring-primary-400'
                        : 'border-surface-200 bg-surface-50 hover:bg-surface-100/60'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className={`text-xs font-bold ${active ? 'text-primary-800' : 'text-surface-900'}`}>
                        {t.label}
                      </span>
                      {active && <CheckCircle className="h-3.5 w-3.5 text-primary-600" />}
                    </div>
                    <p className="text-[11px] text-surface-500 mt-1 leading-snug">{t.desc}</p>
                  </button>
                )
              })}
            </div>
          </div>

          {/* Difficulty & Question Count */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Difficulty */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-surface-700 flex items-center gap-1">
                <Sliders className="h-3.5 w-3.5 text-amber-500" />
                Interview Difficulty
              </label>
              <div className="space-y-2">
                {DIFFICULTIES.map((d) => {
                  const active = difficulty === d.id
                  return (
                    <button
                      type="button"
                      key={d.id}
                      onClick={() => setDifficulty(d.id)}
                      className={`w-full p-2.5 rounded-lg border text-left flex items-center justify-between transition ${
                        active
                          ? 'border-primary-500 bg-primary-50/50 shadow-xs'
                          : 'border-surface-200 bg-surface-50 hover:bg-surface-100/50'
                      }`}
                    >
                      <div>
                        <span className={`text-xs font-bold block ${active ? 'text-primary-800' : 'text-surface-900'}`}>
                          {d.label}
                        </span>
                        <span className="text-[10px] text-surface-400">{d.desc}</span>
                      </div>
                      {active && <CheckCircle className="h-3.5 w-3.5 text-primary-600 flex-shrink-0" />}
                    </button>
                  )
                })}
              </div>
            </div>

            {/* Question Count */}
            <div className="space-y-2">
              <label className="text-xs font-semibold text-surface-700 flex items-center gap-1">
                <Award className="h-3.5 w-3.5 text-emerald-600" />
                Session Length
              </label>
              <div className="space-y-2">
                {QUESTION_COUNTS.map((q) => {
                  const active = totalQuestions === q.count
                  return (
                    <button
                      type="button"
                      key={q.count}
                      onClick={() => setTotalQuestions(q.count)}
                      className={`w-full p-2.5 rounded-lg border text-left flex items-center justify-between transition ${
                        active
                          ? 'border-primary-500 bg-primary-50/50 shadow-xs'
                          : 'border-surface-200 bg-surface-50 hover:bg-surface-100/50'
                      }`}
                    >
                      <div>
                        <span className={`text-xs font-bold block ${active ? 'text-primary-800' : 'text-surface-900'}`}>
                          {q.label}
                        </span>
                        <span className="text-[10px] text-surface-400">Estimated duration: {q.time}</span>
                      </div>
                      {active && <CheckCircle className="h-3.5 w-3.5 text-primary-600 flex-shrink-0" />}
                    </button>
                  )
                })}
              </div>
            </div>
          </div>

          <div className="pt-3 border-t border-surface-100 flex justify-end">
            <button
              type="submit"
              disabled={starting || !targetRole.trim()}
              className="btn-primary btn btn-md inline-flex items-center gap-2"
            >
              {starting ? (
                <>
                  <Sparkles className="h-4 w-4 animate-spin" />
                  <span>Preparing AI Interviewer...</span>
                </>
              ) : (
                <>
                  <Play className="h-4 w-4" />
                  <span>Start Mock Interview</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
