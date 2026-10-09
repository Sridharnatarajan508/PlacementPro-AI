import React, { useState, useEffect, useCallback } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  ClipboardList, Plus, Search, Filter, ArrowUpDown, Clock,
  Calendar, Building2, MapPin, ExternalLink, CheckCircle2,
  AlertCircle, Sparkles, RefreshCw, ChevronRight, MoreVertical,
  Trash2, Edit3, MessageSquare, Bot, Mic, BookOpen, Award,
  ArrowRight, ShieldCheck, CheckSquare, X, Briefcase, Eye,
  Layers, Kanban, List as ListIcon, AlertTriangle, Send, PhoneCall
} from 'lucide-react'
import toast from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { jobApplicationService, applicationIntelligenceService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'

const STATUS_CONFIG = {
  SAVED: {
    label: 'Saved',
    color: 'text-slate-700 bg-slate-100 border-slate-300',
    dot: 'bg-slate-400',
    columnBg: 'bg-slate-50/70 border-slate-200'
  },
  APPLIED: {
    label: 'Applied',
    color: 'text-blue-700 bg-blue-50 border-blue-200',
    dot: 'bg-blue-500',
    columnBg: 'bg-blue-50/30 border-blue-100'
  },
  SCREENING: {
    label: 'Screening',
    color: 'text-amber-700 bg-amber-50 border-amber-200',
    dot: 'bg-amber-500',
    columnBg: 'bg-amber-50/30 border-amber-100'
  },
  INTERVIEW: {
    label: 'Interview',
    color: 'text-purple-700 bg-purple-50 border-purple-200',
    dot: 'bg-purple-500',
    columnBg: 'bg-purple-50/30 border-purple-100'
  },
  OFFER: {
    label: 'Offer',
    color: 'text-emerald-700 bg-emerald-50 border-emerald-300',
    dot: 'bg-emerald-500',
    columnBg: 'bg-emerald-50/40 border-emerald-200'
  },
  REJECTED: {
    label: 'Rejected',
    color: 'text-rose-700 bg-rose-50 border-rose-200',
    dot: 'bg-rose-500',
    columnBg: 'bg-rose-50/20 border-rose-100'
  },
  WITHDRAWN: {
    label: 'Withdrawn',
    color: 'text-gray-700 bg-gray-100 border-gray-300',
    dot: 'bg-gray-400',
    columnBg: 'bg-gray-50/30 border-gray-200'
  }
}

const KANBAN_STAGES = ['SAVED', 'APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER', 'REJECTED']

export default function ApplicationsPage() {
  const navigate = useNavigate()

  const [applications, setApplications] = useState([])
  const [stats, setStats] = useState(null)
  const [upcoming, setUpcoming] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState(null)

  // Filters & View state
  const [viewMode, setViewMode] = useState('kanban') // 'kanban' | 'list'
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [sourceFilter, setSourceFilter] = useState('ALL')
  const [sortBy, setSortBy] = useState('latest')

  // Modals state
  const [selectedApp, setSelectedApp] = useState(null)
  const [detailModalOpen, setDetailModalOpen] = useState(false)
  const [editingNotes, setEditingNotes] = useState('')
  const [savingNotes, setSavingNotes] = useState(false)

  // AI Intelligence state
  const [aiModalOpen, setAiModalOpen] = useState(false)
  const [aiLoading, setAiLoading] = useState(false)
  const [aiData, setAiData] = useState(null)
  const [aiError, setAiError] = useState(null)

  // Custom App creation modal
  const [createModalOpen, setCreateModalOpen] = useState(false)
  const [newAppForm, setNewAppForm] = useState({
    company: '',
    jobTitle: '',
    location: '',
    jobUrl: '',
    source: 'MANUAL_ENTRY',
    status: 'SAVED',
    applicationDate: new Date().toISOString().split('T')[0],
    deadline: '',
    nextFollowUpDate: '',
    interviewDate: '',
    recruiterName: '',
    recruiterEmail: '',
    notes: ''
  })
  const [creatingApp, setCreatingApp] = useState(false)

  // Initial load
  const loadData = useCallback(async (isRefresh = false) => {
    if (isRefresh) setRefreshing(true)
    else setLoading(true)
    setError(null)

    try {
      const [appsRes, statsRes, upcomingRes] = await Promise.all([
        jobApplicationService.getApplications(),
        jobApplicationService.getStats(),
        jobApplicationService.getUpcoming()
      ])

      setApplications(appsRes.data || [])
      setStats(statsRes.data || null)
      setUpcoming(upcomingRes.data || null)
    } catch (err) {
      console.error('Failed to load application data:', err)
      setError(getErrorMessage(err) || 'Failed to load application tracker data. Please try again.')
    } finally {
      setLoading(false)
      setRefreshing(false)
    }
  }, [])

  useEffect(() => {
    loadData()
  }, [loadData])

  // Status Change Handler
  const handleStatusChange = async (appId, newStatus) => {
    try {
      const res = await jobApplicationService.updateStatus(appId, newStatus)
      toast.success(`Application status updated to ${STATUS_CONFIG[newStatus]?.label || newStatus}`)

      // Update local state
      setApplications(prev => prev.map(a => a.id === appId ? { ...a, status: newStatus, updatedAt: new Date().toISOString() } : a))
      if (selectedApp && selectedApp.id === appId) {
        setSelectedApp(prev => ({ ...prev, status: newStatus }))
      }

      // Refresh statistics in background
      jobApplicationService.getStats().then(r => setStats(r.data || null)).catch(() => {})
      jobApplicationService.getUpcoming().then(r => setUpcoming(r.data || null)).catch(() => {})
    } catch (err) {
      console.error('Failed to update status:', err)
      toast.error(getErrorMessage(err) || 'Could not update status')
    }
  }

  // Delete Application Handler
  const handleDeleteApplication = async (appId) => {
    if (!window.confirm('Are you sure you want to delete this job application?')) return

    try {
      await jobApplicationService.deleteApplication(appId)
      toast.success('Application deleted successfully')
      setApplications(prev => prev.filter(a => a.id !== appId))
      setDetailModalOpen(false)
      setSelectedApp(null)

      // Refresh stats
      jobApplicationService.getStats().then(r => setStats(r.data || null)).catch(() => {})
      jobApplicationService.getUpcoming().then(r => setUpcoming(r.data || null)).catch(() => {})
    } catch (err) {
      console.error('Failed to delete application:', err)
      toast.error(getErrorMessage(err) || 'Could not delete application')
    }
  }

  // Open Detail Modal
  const handleOpenDetails = async (app) => {
    setSelectedApp(app)
    setEditingNotes(app.notes || '')
    setDetailModalOpen(true)

    // Fetch fresh details with full activity timeline
    try {
      const res = await jobApplicationService.getApplication(app.id)
      setSelectedApp(res.data)
      setEditingNotes(res.data.notes || '')
    } catch (err) {
      console.error('Failed to load application details:', err)
    }
  }

  // Save Notes Handler
  const handleSaveNotes = async () => {
    if (!selectedApp) return
    setSavingNotes(true)
    try {
      const updatedData = { ...selectedApp, notes: editingNotes }
      const res = await jobApplicationService.updateApplication(selectedApp.id, updatedData)
      setSelectedApp(res.data.application || res.data)
      setApplications(prev => prev.map(a => a.id === selectedApp.id ? { ...a, notes: editingNotes } : a))
      toast.success('Notes saved')
    } catch (err) {
      console.error('Failed to save notes:', err)
      toast.error(getErrorMessage(err) || 'Could not save notes')
    } finally {
      setSavingNotes(false)
    }
  }

  // Open AI Insights Modal
  const handleOpenAiInsights = async (app) => {
    setSelectedApp(app)
    setAiModalOpen(true)
    setAiLoading(true)
    setAiError(null)
    setAiData(null)

    try {
      const res = await applicationIntelligenceService.analyzeApplication(app.id)
      setAiData(res.data)
    } catch (err) {
      console.error('Failed to load AI intelligence:', err)
      const is429 = err?.response?.status === 429 || (err?.message && err.message.includes('429'))
      if (is429) {
        setAiError('AI usage limit reached. Please wait a moment and click Regenerate.')
      } else {
        setAiError(getErrorMessage(err) || 'Could not generate AI Application Insights.')
      }
    } finally {
      setAiLoading(false)
    }
  }

  const handleRegenerateAiInsights = async () => {
    if (!selectedApp) return
    setAiLoading(true)
    setAiError(null)

    try {
      const res = await applicationIntelligenceService.regenerateAnalysis(selectedApp.id)
      setAiData(res.data)
      toast.success('AI insights regenerated!')
    } catch (err) {
      console.error('Failed to regenerate AI intelligence:', err)
      const is429 = err?.response?.status === 429 || (err?.message && err.message.includes('429'))
      if (is429) {
        setAiError('AI usage limit reached. Please wait a moment and click Regenerate.')
      } else {
        setAiError(getErrorMessage(err) || 'Failed to regenerate AI Application Insights.')
      }
    } finally {
      setAiLoading(false)
    }
  }

  // Create Custom Application Handler
  const handleCreateCustomApp = async (e) => {
    e.preventDefault()
    if (!newAppForm.company.trim() || !newAppForm.jobTitle.trim()) {
      toast.error('Company and Job Title are required.')
      return
    }

    setCreatingApp(true)
    try {
      const res = await jobApplicationService.createCustomApplication(newAppForm)
      const created = res.data.application || res.data
      toast.success('Application added to tracker!')
      setApplications(prev => [created, ...prev])
      setCreateModalOpen(false)
      setNewAppForm({
        company: '',
        jobTitle: '',
        location: '',
        jobUrl: '',
        source: 'MANUAL_ENTRY',
        status: 'SAVED',
        applicationDate: new Date().toISOString().split('T')[0],
        deadline: '',
        nextFollowUpDate: '',
        interviewDate: '',
        recruiterName: '',
        recruiterEmail: '',
        notes: ''
      })

      // Refresh stats
      jobApplicationService.getStats().then(r => setStats(r.data || null)).catch(() => {})
      jobApplicationService.getUpcoming().then(r => setUpcoming(r.data || null)).catch(() => {})
    } catch (err) {
      console.error('Failed to create application:', err)
      toast.error(getErrorMessage(err) || 'Could not create application')
    } finally {
      setCreatingApp(false)
    }
  }

  // Filter & Sort Applications
  const filteredApps = applications.filter(app => {
    const matchesSearch = !searchKeyword.trim() ||
      (app.company && app.company.toLowerCase().includes(searchKeyword.toLowerCase())) ||
      (app.jobTitle && app.jobTitle.toLowerCase().includes(searchKeyword.toLowerCase())) ||
      (app.location && app.location.toLowerCase().includes(searchKeyword.toLowerCase()))

    const matchesStatus = statusFilter === 'ALL' || app.status === statusFilter
    const matchesSource = sourceFilter === 'ALL' || app.source === sourceFilter

    return matchesSearch && matchesStatus && matchesSource
  }).sort((a, b) => {
    if (sortBy === 'applicationDate') {
      return (b.applicationDate || '').localeCompare(a.applicationDate || '')
    }
    if (sortBy === 'deadline') {
      return (a.deadline || '9999').localeCompare(b.deadline || '9999')
    }
    if (sortBy === 'nextFollowUpDate') {
      return (a.nextFollowUpDate || '9999').localeCompare(b.nextFollowUpDate || '9999')
    }
    if (sortBy === 'interviewDate') {
      return (a.interviewDate || '9999').localeCompare(b.interviewDate || '9999')
    }
    // Default: latest updated
    return new Date(b.updatedAt || 0) - new Date(a.updatedAt || 0)
  })

  return (
    <AppLayout>
      <div className="space-y-6 pb-12 animate-fade-in">
        
        {/* ============================================================ */}
        {/* HEADER & ACTION BUTTONS                                      */}
        {/* ============================================================ */}
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-primary-600 to-indigo-600 flex items-center justify-center text-white shadow-xs">
                <ClipboardList className="h-5 w-5" />
              </div>
              <h1 className="text-xl font-bold text-surface-900 leading-tight">
                Job Application Tracker
              </h1>
            </div>
            <p className="text-xs text-surface-500 mt-1">
              Track, organize, and strategically prepare for every opportunity from saved job to offer.
            </p>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              onClick={() => loadData(true)}
              disabled={refreshing || loading}
              className="btn-secondary btn btn-sm inline-flex items-center gap-1.5"
              title="Refresh applications"
            >
              <RefreshCw className={`h-3.5 w-3.5 ${refreshing ? 'animate-spin' : ''}`} />
              <span className="hidden sm:inline">Refresh</span>
            </button>

            <button
              onClick={() => setCreateModalOpen(true)}
              className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-xs"
            >
              <Plus className="h-4 w-4" />
              <span>Track Application</span>
            </button>
          </div>
        </div>

        {/* ============================================================ */}
        {/* STATISTICS SUMMARY ROW                                       */}
        {/* ============================================================ */}
        {stats && (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
            {[
              { label: 'Total Tracked', val: stats.totalApplications, color: 'text-slate-900', bg: 'bg-white' },
              { label: 'Applied', val: stats.appliedCount, color: 'text-blue-600', bg: 'bg-blue-50/40' },
              { label: 'Screening', val: stats.screeningCount, color: 'text-amber-600', bg: 'bg-amber-50/40' },
              { label: 'Interviews', val: stats.interviewCount, color: 'text-purple-600', bg: 'bg-purple-50/40' },
              { label: 'Offers', val: stats.offerCount, color: 'text-emerald-600', bg: 'bg-emerald-50/40' },
              { label: 'Rejected', val: stats.rejectedCount, color: 'text-rose-600', bg: 'bg-rose-50/40' },
            ].map((s, idx) => (
              <div key={idx} className={`card p-3.5 border border-surface-200 ${s.bg} text-center space-y-1`}>
                <span className="text-[11px] font-semibold text-surface-500 block uppercase tracking-wider">{s.label}</span>
                <span className={`text-2xl font-extrabold ${s.color}`}>{s.val || 0}</span>
              </div>
            ))}
          </div>
        )}

        {/* Funnel Metrics Banner */}
        {stats && stats.totalApplications > 0 && (
          <div className="card p-4 border border-surface-200 bg-white grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="font-semibold text-surface-700">Response Rate</span>
                <span className="font-bold text-blue-700">{stats.responseRate}%</span>
              </div>
              <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
                <div className="h-full bg-blue-600 rounded-full" style={{ width: `${Math.min(100, stats.responseRate)}%` }} />
              </div>
              <p className="text-[10px] text-surface-400">Screening + Interview from applied</p>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="font-semibold text-surface-700">Interview Rate</span>
                <span className="font-bold text-purple-700">{stats.interviewRate}%</span>
              </div>
              <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
                <div className="h-full bg-purple-600 rounded-full" style={{ width: `${Math.min(100, stats.interviewRate)}%` }} />
              </div>
              <p className="text-[10px] text-surface-400">Rounds progressed to interview</p>
            </div>

            <div className="space-y-1">
              <div className="flex justify-between text-xs">
                <span className="font-semibold text-surface-700">Offer Rate</span>
                <span className="font-bold text-emerald-700">{stats.offerRate}%</span>
              </div>
              <div className="h-2 w-full bg-surface-100 rounded-full overflow-hidden">
                <div className="h-full bg-emerald-600 rounded-full" style={{ width: `${Math.min(100, stats.offerRate)}%` }} />
              </div>
              <p className="text-[10px] text-surface-400">Total job offers received</p>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* UPCOMING TASKS WIDGET                                        */}
        {/* ============================================================ */}
        {upcoming && (upcoming.upcomingFollowUps.length > 0 || upcoming.upcomingInterviews.length > 0 || upcoming.approachingDeadlines.length > 0) && (
          <div className="card p-4 border border-indigo-200 bg-gradient-to-r from-indigo-50/60 via-white to-purple-50/40 space-y-3 shadow-xs">
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-indigo-600" />
              <h3 className="text-xs font-bold text-surface-900 uppercase tracking-wider">
                Upcoming Priority Tasks & Schedule
              </h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Follow ups */}
              {upcoming.upcomingFollowUps.length > 0 && (
                <div className="p-3 bg-white rounded-xl border border-indigo-100 space-y-1.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-indigo-700">
                    <PhoneCall className="h-3.5 w-3.5" />
                    <span>Follow-Up Due ({upcoming.upcomingFollowUps.length})</span>
                  </div>
                  {upcoming.upcomingFollowUps.slice(0, 2).map((item, i) => (
                    <div key={i} onClick={() => handleOpenDetails(item)} className="p-2 rounded-lg bg-surface-50 hover:bg-indigo-50/50 cursor-pointer text-xs transition">
                      <p className="font-bold text-surface-900 truncate">{item.jobTitle}</p>
                      <p className="text-[11px] text-surface-500 truncate">{item.company} • Due: {item.nextFollowUpDate}</p>
                    </div>
                  ))}
                </div>
              )}

              {/* Scheduled Interviews */}
              {upcoming.upcomingInterviews.length > 0 && (
                <div className="p-3 bg-white rounded-xl border border-purple-100 space-y-1.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-purple-700">
                    <Mic className="h-3.5 w-3.5" />
                    <span>Upcoming Interviews ({upcoming.upcomingInterviews.length})</span>
                  </div>
                  {upcoming.upcomingInterviews.slice(0, 2).map((item, i) => (
                    <div key={i} onClick={() => handleOpenDetails(item)} className="p-2 rounded-lg bg-surface-50 hover:bg-purple-50/50 cursor-pointer text-xs transition">
                      <p className="font-bold text-surface-900 truncate">{item.jobTitle}</p>
                      <p className="text-[11px] text-surface-500 truncate">{item.company} • {item.interviewDate ? new Date(item.interviewDate).toLocaleDateString() : 'Scheduled'}</p>
                    </div>
                  ))}
                </div>
              )}

              {/* Deadlines */}
              {upcoming.approachingDeadlines.length > 0 && (
                <div className="p-3 bg-white rounded-xl border border-rose-100 space-y-1.5">
                  <div className="flex items-center gap-1.5 text-xs font-bold text-rose-700">
                    <Calendar className="h-3.5 w-3.5" />
                    <span>Application Deadlines ({upcoming.approachingDeadlines.length})</span>
                  </div>
                  {upcoming.approachingDeadlines.slice(0, 2).map((item, i) => (
                    <div key={i} onClick={() => handleOpenDetails(item)} className="p-2 rounded-lg bg-surface-50 hover:bg-rose-50/50 cursor-pointer text-xs transition">
                      <p className="font-bold text-surface-900 truncate">{item.jobTitle}</p>
                      <p className="text-[11px] text-surface-500 truncate">{item.company} • Deadline: {item.deadline}</p>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* SEARCH, FILTER & VIEW CONTROLS                               */}
        {/* ============================================================ */}
        <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 bg-white p-3.5 rounded-2xl border border-surface-200 shadow-xs">
          
          {/* Search box */}
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
            <input
              type="text"
              placeholder="Search by company, role, or location..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs rounded-xl bg-surface-50 border border-surface-300 focus:outline-none focus:ring-2 focus:ring-primary-100 focus:border-primary-500 transition"
            />
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {/* Status Filter */}
            <select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              className="text-xs py-2 px-3 rounded-xl bg-surface-50 border border-surface-300 text-surface-700 focus:outline-none focus:ring-2 focus:ring-primary-100"
            >
              <option value="ALL">All Statuses</option>
              {Object.entries(STATUS_CONFIG).map(([key, cfg]) => (
                <option key={key} value={key}>{cfg.label}</option>
              ))}
            </select>

            {/* Sort Filter */}
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="text-xs py-2 px-3 rounded-xl bg-surface-50 border border-surface-300 text-surface-700 focus:outline-none focus:ring-2 focus:ring-primary-100"
            >
              <option value="latest">Latest Updated</option>
              <option value="applicationDate">Application Date</option>
              <option value="deadline">Approaching Deadline</option>
              <option value="nextFollowUpDate">Next Follow-Up</option>
              <option value="interviewDate">Interview Date</option>
            </select>

            {/* View Mode Toggle */}
            <div className="flex items-center bg-surface-100 p-1 rounded-xl border border-surface-200">
              <button
                onClick={() => setViewMode('kanban')}
                className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition ${
                  viewMode === 'kanban' ? 'bg-white text-primary-700 shadow-xs' : 'text-surface-600 hover:text-surface-900'
                }`}
                title="Kanban Board View"
              >
                <Kanban className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">Pipeline</span>
              </button>

              <button
                onClick={() => setViewMode('list')}
                className={`p-1.5 rounded-lg text-xs font-semibold flex items-center gap-1 transition ${
                  viewMode === 'list' ? 'bg-white text-primary-700 shadow-xs' : 'text-surface-600 hover:text-surface-900'
                }`}
                title="Table List View"
              >
                <ListIcon className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">List</span>
              </button>
            </div>
          </div>
        </div>

        {/* ============================================================ */}
        {/* MAIN CONTENT: KANBAN PIPELINE OR LIST VIEW                   */}
        {/* ============================================================ */}
        {loading ? (
          <LoadingSpinner text="Loading job application tracker..." />
        ) : error ? (
          <div className="card p-8 text-center space-y-4">
            <AlertTriangle className="h-10 w-10 text-rose-500 mx-auto" />
            <p className="text-sm font-semibold text-surface-900">{error}</p>
            <button onClick={() => loadData()} className="btn-primary btn btn-sm inline-flex items-center gap-1.5 mx-auto">
              <RefreshCw className="h-4 w-4" />
              <span>Retry</span>
            </button>
          </div>
        ) : applications.length === 0 ? (
          <EmptyState
            icon={<ClipboardList className="text-primary-500" />}
            title="No Applications Tracked Yet"
            description="Start tracking jobs from the Job Matcher or add custom applications to manage your complete interview and offer campaign."
            action={
              <div className="flex gap-2">
                <Link to="/jobs" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                  <Briefcase className="h-3.5 w-3.5" />
                  <span>Browse Jobs</span>
                </Link>
                <button onClick={() => setCreateModalOpen(true)} className="btn-secondary btn btn-sm inline-flex items-center gap-1.5">
                  <Plus className="h-3.5 w-3.5" />
                  <span>Add Custom Job</span>
                </button>
              </div>
            }
          />
        ) : viewMode === 'kanban' ? (
          /* ========================================================== */
          /* KANBAN BOARD VIEW                                          */
          /* ========================================================== */
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3.5 items-start overflow-x-auto pb-4">
            {KANBAN_STAGES.map((stageKey) => {
              const stageConfig = STATUS_CONFIG[stageKey] || {}
              const stageApps = filteredApps.filter(a => a.status === stageKey)

              return (
                <div
                  key={stageKey}
                  className={`rounded-2xl border ${stageConfig.columnBg} p-3 flex flex-col min-h-[450px] shadow-2xs`}
                >
                  {/* Column Header */}
                  <div className="flex items-center justify-between pb-2.5 mb-2.5 border-b border-surface-200/80">
                    <div className="flex items-center gap-2">
                      <span className={`w-2.5 h-2.5 rounded-full ${stageConfig.dot}`} />
                      <h4 className="text-xs font-bold text-surface-900">{stageConfig.label}</h4>
                    </div>
                    <span className="text-[11px] font-mono px-2 py-0.5 rounded-full bg-white border border-surface-200 text-surface-700 font-bold">
                      {stageApps.length}
                    </span>
                  </div>

                  {/* Cards List */}
                  <div className="space-y-2.5 flex-1">
                    {stageApps.length === 0 ? (
                      <div className="h-32 border-2 border-dashed border-surface-200 rounded-xl flex items-center justify-center text-[11px] text-surface-400 italic">
                        No applications
                      </div>
                    ) : (
                      stageApps.map((app) => (
                        <div
                          key={app.id}
                          className="card p-3.5 border border-surface-200 bg-white hover:border-primary-300 hover:shadow-xs transition space-y-2.5 group rounded-xl"
                        >
                          {/* Role & Company */}
                          <div>
                            <div className="flex items-start justify-between gap-1">
                              <h5
                                onClick={() => handleOpenDetails(app)}
                                className="text-xs font-bold text-surface-900 hover:text-primary-600 transition cursor-pointer leading-tight"
                              >
                                {app.jobTitle}
                              </h5>
                            </div>
                            <p className="text-[11px] text-surface-600 flex items-center gap-1 mt-0.5">
                              <Building2 className="h-3 w-3 text-surface-400 flex-shrink-0" />
                              <span className="truncate">{app.company}</span>
                            </p>
                          </div>

                          {/* Meta pill badges */}
                          <div className="flex flex-wrap gap-1 text-[10px]">
                            {app.matchScore != null && (
                              <span className="px-1.5 py-0.5 rounded bg-emerald-50 text-emerald-700 font-bold border border-emerald-200">
                                {app.matchScore}% Match
                              </span>
                            )}
                            {app.location && (
                              <span className="px-1.5 py-0.5 rounded bg-surface-100 text-surface-600 truncate max-w-[120px]">
                                {app.location}
                              </span>
                            )}
                          </div>

                          {/* Dates Indicator */}
                          {(app.interviewDate || app.nextFollowUpDate || app.deadline) && (
                            <div className="text-[10px] space-y-0.5 pt-1 border-t border-surface-100 text-surface-500">
                              {app.interviewDate && (
                                <p className="text-purple-700 font-semibold flex items-center gap-1">
                                  <Mic className="h-2.5 w-2.5" />
                                  <span>Interview: {new Date(app.interviewDate).toLocaleDateString()}</span>
                                </p>
                              )}
                              {app.nextFollowUpDate && (
                                <p className="text-indigo-600 font-medium flex items-center gap-1">
                                  <Clock className="h-2.5 w-2.5" />
                                  <span>Follow-up: {app.nextFollowUpDate}</span>
                                </p>
                              )}
                              {app.deadline && (
                                <p className="text-rose-600 font-medium flex items-center gap-1">
                                  <Calendar className="h-2.5 w-2.5" />
                                  <span>Deadline: {app.deadline}</span>
                                </p>
                              )}
                            </div>
                          )}

                          {/* Card Action Buttons */}
                          <div className="pt-2 border-t border-surface-100 flex items-center justify-between gap-1">
                            <button
                              onClick={() => handleOpenAiInsights(app)}
                              className="p-1 rounded text-primary-600 hover:bg-primary-50 text-[11px] font-semibold flex items-center gap-1"
                              title="AI Application Strategy"
                            >
                              <Sparkles className="h-3 w-3" />
                              <span>AI Strategy</span>
                            </button>

                            <select
                              value={app.status}
                              onChange={(e) => handleStatusChange(app.id, e.target.value)}
                              className="text-[10px] py-1 px-1.5 rounded-md bg-surface-50 border border-surface-200 text-surface-700 font-medium focus:outline-none"
                            >
                              {Object.entries(STATUS_CONFIG).map(([k, cfg]) => (
                                <option key={k} value={k}>{cfg.label}</option>
                              ))}
                            </select>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          /* ========================================================== */
          /* TABLE / LIST VIEW                                          */
          /* ========================================================== */
          <div className="card overflow-hidden border border-surface-200 shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-surface-50 border-b border-surface-200 text-[11px] text-surface-500 font-bold uppercase">
                  <tr>
                    <th className="py-3 px-4">Role & Company</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Match</th>
                    <th className="py-3 px-4">Timeline / Dates</th>
                    <th className="py-3 px-4">Source</th>
                    <th className="py-3 px-4 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-surface-100 bg-white">
                  {filteredApps.map((app) => {
                    const statusCfg = STATUS_CONFIG[app.status] || {}
                    return (
                      <tr key={app.id} className="hover:bg-surface-50/70 transition">
                        <td className="py-3.5 px-4">
                          <div className="space-y-0.5">
                            <p
                              onClick={() => handleOpenDetails(app)}
                              className="font-bold text-surface-900 hover:text-primary-600 cursor-pointer text-xs"
                            >
                              {app.jobTitle}
                            </p>
                            <p className="text-[11px] text-surface-500">{app.company} {app.location ? `• ${app.location}` : ''}</p>
                          </div>
                        </td>

                        <td className="py-3.5 px-4">
                          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-bold border ${statusCfg.color}`}>
                            {statusCfg.label}
                          </span>
                        </td>

                        <td className="py-3.5 px-4">
                          {app.matchScore != null ? (
                            <span className="font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded text-[11px]">
                              {app.matchScore}%
                            </span>
                          ) : (
                            <span className="text-surface-400 text-[11px]">—</span>
                          )}
                        </td>

                        <td className="py-3.5 px-4 text-[11px] text-surface-600 space-y-0.5">
                          <p>Applied: {app.applicationDate || '—'}</p>
                          {app.interviewDate && (
                            <p className="text-purple-700 font-bold">Interview: {new Date(app.interviewDate).toLocaleDateString()}</p>
                          )}
                          {app.nextFollowUpDate && (
                            <p className="text-indigo-600 font-medium">Follow-up: {app.nextFollowUpDate}</p>
                          )}
                        </td>

                        <td className="py-3.5 px-4 text-[11px] text-surface-500">
                          {app.source || 'CAREER_COPILOT'}
                        </td>

                        <td className="py-3.5 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              onClick={() => handleOpenAiInsights(app)}
                              className="btn-secondary btn btn-xs inline-flex items-center gap-1 text-primary-700 font-semibold"
                            >
                              <Sparkles className="h-3 w-3" />
                              <span>AI Strategy</span>
                            </button>

                            <button
                              onClick={() => handleOpenDetails(app)}
                              className="btn-secondary btn btn-xs"
                            >
                              Details
                            </button>
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* MODAL 1: APPLICATION DETAILS & EDIT MODAL                    */}
        {/* ============================================================ */}
        {detailModalOpen && selectedApp && (
          <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-xl border border-surface-200 animate-slide-up">
              
              {/* Modal Header */}
              <div className="p-5 border-b border-surface-200 flex items-start justify-between bg-surface-50/50 rounded-t-2xl">
                <div>
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${STATUS_CONFIG[selectedApp.status]?.color}`}>
                      {STATUS_CONFIG[selectedApp.status]?.label}
                    </span>
                    {selectedApp.matchScore != null && (
                      <span className="text-[10px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded border border-emerald-200">
                        {selectedApp.matchScore}% Match
                      </span>
                    )}
                  </div>
                  <h3 className="text-base font-bold text-surface-900 mt-1">{selectedApp.jobTitle}</h3>
                  <p className="text-xs text-surface-600">{selectedApp.company} {selectedApp.location ? `• ${selectedApp.location}` : ''}</p>
                </div>

                <button onClick={() => setDetailModalOpen(false)} className="p-1 rounded-lg hover:bg-surface-200 text-surface-400">
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* Modal Body */}
              <div className="p-5 overflow-y-auto space-y-5 flex-1 text-xs">
                
                {/* Status & Quick Links */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 p-3.5 bg-surface-50 rounded-xl border border-surface-200">
                  <div>
                    <label className="text-[11px] font-bold text-surface-600 block mb-1">Update Status</label>
                    <select
                      value={selectedApp.status}
                      onChange={(e) => handleStatusChange(selectedApp.id, e.target.value)}
                      className="w-full py-1.5 px-2.5 rounded-lg bg-white border border-surface-300 text-xs font-semibold"
                    >
                      {Object.entries(STATUS_CONFIG).map(([k, cfg]) => (
                        <option key={k} value={k}>{cfg.label}</option>
                      ))}
                    </select>
                  </div>

                  <div className="flex items-end gap-2">
                    {selectedApp.jobId && (
                      <Link
                        to={`/preparation?jobId=${selectedApp.jobId}`}
                        className="btn-secondary btn btn-xs flex-1 inline-flex items-center justify-center gap-1 text-primary-700 font-semibold"
                      >
                        <BookOpen className="h-3 w-3" />
                        <span>Job Prep</span>
                      </Link>
                    )}
                    <Link
                      to={`/mock-interview?targetRole=${encodeURIComponent(selectedApp.jobTitle)}`}
                      className="btn-secondary btn btn-xs flex-1 inline-flex items-center justify-center gap-1 text-purple-700 font-semibold"
                    >
                      <Mic className="h-3 w-3" />
                      <span>Mock Interview</span>
                    </Link>
                  </div>
                </div>

                {/* Dates & Contacts Grid */}
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
                  <div className="p-2.5 rounded-xl border border-surface-200 bg-white">
                    <span className="text-[10px] text-surface-400 font-semibold uppercase block">Applied Date</span>
                    <span className="font-bold text-surface-800">{selectedApp.applicationDate || '—'}</span>
                  </div>
                  <div className="p-2.5 rounded-xl border border-surface-200 bg-white">
                    <span className="text-[10px] text-surface-400 font-semibold uppercase block">Deadline</span>
                    <span className="font-bold text-rose-700">{selectedApp.deadline || '—'}</span>
                  </div>
                  <div className="p-2.5 rounded-xl border border-surface-200 bg-white">
                    <span className="text-[10px] text-surface-400 font-semibold uppercase block">Next Follow-Up</span>
                    <span className="font-bold text-indigo-700">{selectedApp.nextFollowUpDate || '—'}</span>
                  </div>
                  <div className="p-2.5 rounded-xl border border-surface-200 bg-white">
                    <span className="text-[10px] text-surface-400 font-semibold uppercase block">Interview Date</span>
                    <span className="font-bold text-purple-700">
                      {selectedApp.interviewDate ? new Date(selectedApp.interviewDate).toLocaleDateString() : '—'}
                    </span>
                  </div>
                </div>

                {/* Recruiter info */}
                {(selectedApp.recruiterName || selectedApp.recruiterEmail) && (
                  <div className="p-3 bg-surface-50 rounded-xl border border-surface-200 flex items-center justify-between text-xs">
                    <div>
                      <span className="text-[10px] text-surface-400 font-semibold uppercase block">Recruiter Contact</span>
                      <span className="font-bold text-surface-900">{selectedApp.recruiterName || 'Contact'}</span>
                      {selectedApp.recruiterEmail && <span className="text-surface-500 ml-2">({selectedApp.recruiterEmail})</span>}
                    </div>
                    {selectedApp.recruiterEmail && (
                      <a href={`mailto:${selectedApp.recruiterEmail}`} className="btn-secondary btn btn-xs">
                        Email Recruiter
                      </a>
                    )}
                  </div>
                )}

                {/* Notes Section */}
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-surface-900">Application Notes & Insights</label>
                    <button
                      onClick={handleSaveNotes}
                      disabled={savingNotes}
                      className="btn-primary btn btn-xs inline-flex items-center gap-1"
                    >
                      {savingNotes ? <RefreshCw className="h-3 w-3 animate-spin" /> : <Edit3 className="h-3 w-3" />}
                      <span>Save Notes</span>
                    </button>
                  </div>
                  <textarea
                    rows={3}
                    value={editingNotes}
                    onChange={(e) => setEditingNotes(e.target.value)}
                    placeholder="Add interview notes, recruiter feedback, compensation details, or referral contacts..."
                    className="w-full p-2.5 rounded-xl border border-surface-300 text-xs focus:ring-2 focus:ring-primary-100 focus:border-primary-500"
                  />
                </div>

                {/* Activity Timeline */}
                {selectedApp.activityTimeline && selectedApp.activityTimeline.length > 0 && (
                  <div className="space-y-2 pt-2 border-t border-surface-200">
                    <h4 className="text-xs font-bold text-surface-900 flex items-center gap-1.5">
                      <Clock className="h-3.5 w-3.5 text-primary-600" />
                      <span>Activity Timeline</span>
                    </h4>
                    <div className="space-y-2 max-h-44 overflow-y-auto pr-1">
                      {selectedApp.activityTimeline.map((act) => (
                        <div key={act.id} className="p-2 rounded-lg bg-surface-50 border border-surface-100 text-[11px] flex items-start justify-between gap-2">
                          <div>
                            <span className="font-semibold text-surface-800">{act.description}</span>
                            <span className="text-[10px] text-surface-400 block mt-0.5">
                              {act.activityDate ? new Date(act.activityDate).toLocaleString() : ''}
                            </span>
                          </div>
                          <span className="px-1.5 py-0.5 rounded bg-surface-200/60 text-surface-700 text-[9px] font-bold">
                            {act.activityType}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Modal Footer */}
              <div className="p-4 border-t border-surface-200 bg-surface-50/50 rounded-b-2xl flex items-center justify-between">
                <button
                  onClick={() => handleDeleteApplication(selectedApp.id)}
                  className="btn-danger btn btn-xs inline-flex items-center gap-1"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>Delete Application</span>
                </button>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      setDetailModalOpen(false)
                      handleOpenAiInsights(selectedApp)
                    }}
                    className="btn-primary btn btn-xs inline-flex items-center gap-1 shadow-xs"
                  >
                    <Sparkles className="h-3.5 w-3.5" />
                    <span>AI Strategy</span>
                  </button>
                  <button onClick={() => setDetailModalOpen(false)} className="btn-secondary btn btn-xs">
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* MODAL 2: AI APPLICATION INSIGHTS MODAL                       */}
        {/* ============================================================ */}
        {aiModalOpen && selectedApp && (
          <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-xl border border-indigo-100 animate-slide-up">
              
              {/* AI Modal Header */}
              <div className="p-5 border-b border-surface-200 flex items-start justify-between bg-gradient-to-r from-indigo-50/80 via-white to-purple-50/60 rounded-t-2xl">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-primary-600 to-indigo-600 flex items-center justify-center text-white shadow-xs">
                    <Sparkles className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-base font-bold text-surface-900">AI Application Strategy & Guidance</h3>
                    <p className="text-xs text-surface-500">{selectedApp.jobTitle} at {selectedApp.company}</p>
                  </div>
                </div>

                <button onClick={() => setAiModalOpen(false)} className="p-1 rounded-lg hover:bg-surface-200 text-surface-400">
                  <X className="h-5 w-5" />
                </button>
              </div>

              {/* AI Modal Body */}
              <div className="p-5 overflow-y-auto space-y-4 flex-1 text-xs">
                {aiLoading ? (
                  <div className="py-12 text-center space-y-3">
                    <RefreshCw className="h-8 w-8 animate-spin mx-auto text-primary-600" />
                    <p className="text-sm font-bold text-surface-900">Analyzing job fit & interview conversion strategy...</p>
                    <p className="text-xs text-surface-500 max-w-sm mx-auto">
                      Synthesizing your resume, verified skills, job requirements, and interview readiness.
                    </p>
                  </div>
                ) : aiError ? (
                  <div className="p-4 rounded-xl border border-rose-200 bg-rose-50 text-rose-900 space-y-3">
                    <p className="font-bold">{aiError}</p>
                    <button onClick={handleRegenerateAiInsights} className="btn-secondary btn btn-xs inline-flex items-center gap-1">
                      <RefreshCw className="h-3 w-3" />
                      <span>Retry Analysis</span>
                    </button>
                  </div>
                ) : aiData ? (
                  <div className="space-y-4">
                    
                    {/* Score & Summary Banner */}
                    <div className="p-4 rounded-xl border border-indigo-200 bg-indigo-50/30 flex items-center gap-4">
                      <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white flex flex-col items-center justify-center font-extrabold shadow-xs flex-shrink-0">
                        <span className="text-xl leading-none">{aiData.readinessScore || 0}%</span>
                        <span className="text-[9px] uppercase tracking-wider opacity-90 mt-0.5">FIT</span>
                      </div>
                      <div className="space-y-1">
                        <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-indigo-100 text-indigo-800">
                          Priority: {aiData.priority || 'HIGH'}
                        </span>
                        <p className="text-xs text-surface-700 leading-relaxed font-medium">
                          {aiData.summary}
                        </p>
                      </div>
                    </div>

                    {/* Strengths & Risks */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {/* Strengths */}
                      <div className="p-3.5 rounded-xl border border-surface-200 bg-white space-y-2">
                        <h4 className="text-xs font-bold text-emerald-800 flex items-center gap-1.5">
                          <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                          <span>Candidate Advantages</span>
                        </h4>
                        <ul className="space-y-1 text-xs text-surface-600">
                          {aiData.strengths?.map((s, i) => (
                            <li key={i} className="flex items-start gap-1.5">
                              <span className="text-emerald-600 font-bold">•</span>
                              <span>{s}</span>
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Risks */}
                      <div className="p-3.5 rounded-xl border border-surface-200 bg-white space-y-2">
                        <h4 className="text-xs font-bold text-amber-800 flex items-center gap-1.5">
                          <AlertCircle className="h-4 w-4 text-amber-600" />
                          <span>Risks & Gaps to Bridge</span>
                        </h4>
                        <ul className="space-y-1 text-xs text-surface-600">
                          {aiData.risks?.map((r, i) => (
                            <li key={i} className="flex items-start gap-1.5">
                              <span className="text-amber-600 font-bold">•</span>
                              <span>{r}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    {/* Recommended Next Actions */}
                    {aiData.recommendedActions && aiData.recommendedActions.length > 0 && (
                      <div className="p-3.5 rounded-xl border border-surface-200 bg-white space-y-2">
                        <h4 className="text-xs font-bold text-surface-900 flex items-center gap-1.5">
                          <Sparkles className="h-4 w-4 text-primary-600" />
                          <span>Recommended Action Items</span>
                        </h4>
                        <div className="space-y-1.5">
                          {aiData.recommendedActions.map((act, i) => (
                            <div key={i} className="p-2 rounded-lg bg-surface-50 border border-surface-100 flex items-start gap-2">
                              <span className="w-4 h-4 rounded-full bg-primary-100 text-primary-700 font-bold text-[10px] flex items-center justify-center flex-shrink-0 mt-0.5">
                                {i + 1}
                              </span>
                              <span className="text-surface-800 font-medium">{act}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {/* Follow-up Advice & Interview Advice */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                      {aiData.followUpAdvice && aiData.followUpAdvice.length > 0 && (
                        <div className="p-3 rounded-xl border border-surface-200 bg-surface-50 space-y-1.5">
                          <h5 className="text-[11px] font-bold text-indigo-900">Follow-Up Timing & Outreach</h5>
                          <ul className="space-y-1 text-[11px] text-surface-600">
                            {aiData.followUpAdvice.map((f, i) => (
                              <li key={i}>✓ {f}</li>
                            ))}
                          </ul>
                        </div>
                      )}

                      {aiData.interviewAdvice && aiData.interviewAdvice.length > 0 && (
                        <div className="p-3 rounded-xl border border-surface-200 bg-surface-50 space-y-1.5">
                          <h5 className="text-[11px] font-bold text-purple-900">Interview Focus Topics</h5>
                          <ul className="space-y-1 text-[11px] text-surface-600">
                            {aiData.interviewAdvice.map((inv, i) => (
                              <li key={i}>✓ {inv}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  </div>
                ) : null}
              </div>

              {/* AI Modal Footer */}
              <div className="p-4 border-t border-surface-200 bg-surface-50/50 rounded-b-2xl flex items-center justify-between">
                <button
                  onClick={handleRegenerateAiInsights}
                  disabled={aiLoading}
                  className="btn-secondary btn btn-xs inline-flex items-center gap-1.5"
                >
                  <RefreshCw className={`h-3 w-3 ${aiLoading ? 'animate-spin' : ''}`} />
                  <span>Regenerate Strategy</span>
                </button>

                <div className="flex items-center gap-2">
                  <Link
                    to={`/ai-assistant?prompt=${encodeURIComponent(`How should I tailor my application strategy for ${selectedApp.jobTitle} at ${selectedApp.company}?`)}`}
                    className="btn-primary btn btn-xs inline-flex items-center gap-1 shadow-xs"
                  >
                    <Bot className="h-3 w-3" />
                    <span>Ask AI Assistant</span>
                  </Link>
                  <button onClick={() => setAiModalOpen(false)} className="btn-secondary btn btn-xs">
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* ============================================================ */}
        {/* MODAL 3: TRACK CUSTOM APPLICATION MODAL                      */}
        {/* ============================================================ */}
        {createModalOpen && (
          <div className="fixed inset-0 z-50 bg-black/40 backdrop-blur-xs flex items-center justify-center p-4 overflow-y-auto">
            <div className="bg-white rounded-2xl max-w-lg w-full max-h-[90vh] flex flex-col shadow-xl border border-surface-200 animate-slide-up">
              
              <div className="p-5 border-b border-surface-200 flex items-start justify-between bg-surface-50/50 rounded-t-2xl">
                <div>
                  <h3 className="text-base font-bold text-surface-900">Track Custom Job Application</h3>
                  <p className="text-xs text-surface-500">Add an opportunity from LinkedIn, referral, or external job portals.</p>
                </div>
                <button onClick={() => setCreateModalOpen(false)} className="p-1 rounded-lg hover:bg-surface-200 text-surface-400">
                  <X className="h-5 w-5" />
                </button>
              </div>

              <form onSubmit={handleCreateCustomApp} className="p-5 overflow-y-auto space-y-3.5 flex-1 text-xs">
                <div>
                  <label className="font-bold text-surface-700 block mb-1">Company Name *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Google, Microsoft, Startup Inc."
                    value={newAppForm.company}
                    onChange={(e) => setNewAppForm({ ...newAppForm, company: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-surface-300 text-xs focus:ring-2 focus:ring-primary-100"
                  />
                </div>

                <div>
                  <label className="font-bold text-surface-700 block mb-1">Job Title *</label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Java Backend Engineer"
                    value={newAppForm.jobTitle}
                    onChange={(e) => setNewAppForm({ ...newAppForm, jobTitle: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-surface-300 text-xs focus:ring-2 focus:ring-primary-100"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Location</label>
                    <input
                      type="text"
                      placeholder="e.g. Bengaluru / Remote"
                      value={newAppForm.location}
                      onChange={(e) => setNewAppForm({ ...newAppForm, location: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                    />
                  </div>

                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Initial Status</label>
                    <select
                      value={newAppForm.status}
                      onChange={(e) => setNewAppForm({ ...newAppForm, status: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs font-semibold"
                    >
                      {Object.entries(STATUS_CONFIG).map(([k, cfg]) => (
                        <option key={k} value={k}>{cfg.label}</option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Application Date</label>
                    <input
                      type="date"
                      value={newAppForm.applicationDate}
                      onChange={(e) => setNewAppForm({ ...newAppForm, applicationDate: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                    />
                  </div>

                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Deadline Date</label>
                    <input
                      type="date"
                      value={newAppForm.deadline}
                      onChange={(e) => setNewAppForm({ ...newAppForm, deadline: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                    />
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Follow-Up Date</label>
                    <input
                      type="date"
                      value={newAppForm.nextFollowUpDate}
                      onChange={(e) => setNewAppForm({ ...newAppForm, nextFollowUpDate: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                    />
                  </div>

                  <div>
                    <label className="font-bold text-surface-700 block mb-1">Source</label>
                    <select
                      value={newAppForm.source}
                      onChange={(e) => setNewAppForm({ ...newAppForm, source: e.target.value })}
                      className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                    >
                      <option value="LINKEDIN">LinkedIn</option>
                      <option value="INDEED">Indeed</option>
                      <option value="COMPANY_WEBSITE">Company Website</option>
                      <option value="REFERRAL">Referral / Network</option>
                      <option value="MANUAL_ENTRY">Manual Entry</option>
                    </select>
                  </div>
                </div>

                <div>
                  <label className="font-bold text-surface-700 block mb-1">Job Link / URL</label>
                  <input
                    type="url"
                    placeholder="https://..."
                    value={newAppForm.jobUrl}
                    onChange={(e) => setNewAppForm({ ...newAppForm, jobUrl: e.target.value })}
                    className="w-full p-2.5 rounded-xl border border-surface-300 text-xs"
                  />
                </div>

                <div className="p-4 border-t border-surface-200 bg-surface-50/50 rounded-b-2xl flex items-center justify-end gap-2 -mx-5 -mb-5 mt-4">
                  <button type="button" onClick={() => setCreateModalOpen(false)} className="btn-secondary btn btn-xs">
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={creatingApp}
                    className="btn-primary btn btn-xs inline-flex items-center gap-1.5 shadow-xs"
                  >
                    {creatingApp ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Plus className="h-3.5 w-3.5" />}
                    <span>Save Application</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  )
}
