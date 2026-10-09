import { useState, useEffect, useRef } from 'react'
import { resumeService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import AppLayout from '../components/layout/AppLayout'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import {
  Upload, FileText, Trash2, RefreshCw, CheckCircle2, AlertTriangle,
  FileCheck, Copy, Check, Eye, EyeOff, Sparkles, AlertCircle,
  TrendingUp, Award, Zap, Target, BookOpen, Layers, Lightbulb,
  CheckCircle, HelpCircle, ArrowRight
} from 'lucide-react'
import toast from 'react-hot-toast'

export default function ResumePage() {
  const [resume, setResume] = useState(null)
  const [analysis, setAnalysis] = useState(null)
  const [loading, setLoading] = useState(true)
  const [analyzing, setAnalyzing] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [selectedFile, setSelectedFile] = useState(null)
  const [dragActive, setDragActive] = useState(false)
  const [showExtractedText, setShowExtractedText] = useState(false)
  const [copied, setCopied] = useState(false)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const [activeSectionTab, setActiveSectionTab] = useState('summary')

  const fileInputRef = useRef(null)

  useEffect(() => {
    fetchLatestResume()
  }, [])

  const fetchLatestResume = async () => {
    setLoading(true)
    try {
      const { data } = await resumeService.getLatestResume()
      setResume(data)
      // Fetch stored analysis if exists
      if (data && data.id) {
        fetchStoredAnalysis(data.id)
      }
    } catch (err) {
      if (err.response?.status !== 404) {
        toast.error(getErrorMessage(err))
      }
      setResume(null)
      setAnalysis(null)
    } finally {
      setLoading(false)
    }
  }

  const fetchStoredAnalysis = async (resumeId) => {
    try {
      const { data } = await resumeService.getResumeAnalysis(resumeId)
      setAnalysis(data)
    } catch (err) {
      // 404 means no analysis performed yet for this resume
      setAnalysis(null)
    }
  }

  const handleAnalyzeResume = async () => {
    if (!resume?.id) return
    setAnalyzing(true)
    try {
      const { data } = await resumeService.analyzeResume(resume.id)
      setAnalysis(data)
      toast.success('AI Resume Analysis generated successfully!')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setAnalyzing(false)
    }
  }

  const validateFile = (file) => {
    if (!file) return false
    const isPdfMime = file.type === 'application/pdf'
    const isPdfExt = file.name.toLowerCase().endsWith('.pdf')

    if (!isPdfMime && !isPdfExt) {
      toast.error('Invalid file format. Only PDF files are allowed.')
      return false
    }

    if (file.size > 5 * 1024 * 1024) {
      toast.error('File size exceeds the 5 MB limit.')
      return false
    }

    return true
  }

  const handleFileSelect = (file) => {
    if (validateFile(file)) {
      setSelectedFile(file)
    }
  }

  const handleDrag = (e) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }

  const handleDrop = (e) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelect(e.dataTransfer.files[0])
    }
  }

  const handleUploadSubmit = async () => {
    if (!selectedFile) return
    setUploading(true)
    try {
      const { data } = await resumeService.uploadResume(selectedFile)
      setResume(data)
      setAnalysis(null) // reset previous analysis
      setSelectedFile(null)
      toast.success('Resume uploaded and text extracted using Apache PDFBox!')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setUploading(false)
    }
  }

  const handleDeleteResume = async () => {
    if (!resume) return
    setDeleting(true)
    try {
      await resumeService.deleteResume(resume.id)
      setResume(null)
      setAnalysis(null)
      setShowDeleteModal(false)
      toast.success('Resume deleted successfully.')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setDeleting(false)
    }
  }

  const handleCopyText = () => {
    if (resume?.extractedText) {
      navigator.clipboard.writeText(resume.extractedText)
      setCopied(true)
      toast.success('Extracted text copied to clipboard!')
      setTimeout(() => setCopied(false), 2000)
    }
  }

  // Group detected skills by category
  const groupedSkills = (analysis?.detectedSkills || []).reduce((acc, skill) => {
    const cat = skill.category || 'Other'
    if (!acc[cat]) acc[cat] = []
    acc[cat].push(skill)
    return acc
  }, {})

  const getPriorityBadgeClass = (priority) => {
    switch (priority?.toUpperCase()) {
      case 'HIGH': return 'badge-danger'
      case 'MEDIUM': return 'badge-warning'
      default: return 'badge-surface'
    }
  }

  const getScoreColorClass = (score) => {
    if (score >= 80) return 'text-success-600 border-success-500 bg-success-50'
    if (score >= 60) return 'text-primary-600 border-primary-500 bg-primary-50'
    return 'text-amber-600 border-amber-500 bg-amber-50'
  }

  return (
    <AppLayout>
      <div className="space-y-6 animate-slide-up max-w-5xl mx-auto">
        {/* Page Header */}
        <div className="page-header">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-primary-100 flex items-center justify-center">
              <FileText className="h-5 w-5 text-primary-600" />
            </div>
            <div>
              <h2 className="page-title">AI Resume Analyzer</h2>
              <p className="page-subtitle">
                Apache PDFBox Text Extraction &amp; Google Gemini AI Analysis
              </p>
            </div>
          </div>
        </div>

        {loading ? (
          <LoadingSpinner text="Loading resume &amp; AI analysis data..." />
        ) : (
          <>
            {/* Upload Area / Replace Area */}
            {(!resume || selectedFile) && (
              <div className="card p-6 sm:p-8 space-y-6">
                <h3 className="text-base font-semibold text-surface-800">
                  {resume ? 'Replace Resume (Upload New PDF)' : 'Upload Your PDF Resume'}
                </h3>

                {/* Drag and Drop Zone */}
                <div
                  onDragEnter={handleDrag}
                  onDragLeave={handleDrag}
                  onDragOver={handleDrag}
                  onDrop={handleDrop}
                  onClick={() => fileInputRef.current?.click()}
                  className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer transition-all ${
                    dragActive
                      ? 'border-primary-500 bg-primary-50/50'
                      : selectedFile
                      ? 'border-success-400 bg-success-50/30'
                      : 'border-surface-300 hover:border-primary-400 hover:bg-surface-50'
                  }`}
                >
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".pdf,application/pdf"
                    className="hidden"
                    onChange={(e) => {
                      if (e.target.files && e.target.files[0]) {
                        handleFileSelect(e.target.files[0])
                      }
                    }}
                  />

                  {selectedFile ? (
                    <div className="flex flex-col items-center gap-2">
                      <FileCheck className="h-12 w-12 text-success-500" />
                      <p className="font-medium text-surface-800 text-sm">{selectedFile.name}</p>
                      <p className="text-xs text-surface-500">
                        {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
                      </p>
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation()
                          setSelectedFile(null)
                        }}
                        className="text-xs text-danger-600 hover:underline mt-1"
                      >
                        Remove file
                      </button>
                    </div>
                  ) : (
                    <div className="flex flex-col items-center gap-2">
                      <div className="w-12 h-12 rounded-full bg-surface-100 flex items-center justify-center">
                        <Upload className="h-6 w-6 text-surface-500" />
                      </div>
                      <p className="text-sm font-medium text-surface-700">
                        Click to upload or drag &amp; drop your resume PDF
                      </p>
                      <p className="text-xs text-surface-400">PDF format only (Max 5 MB)</p>
                    </div>
                  )}
                </div>

                {/* Upload Action Buttons */}
                {selectedFile && (
                  <div className="flex gap-3 justify-end">
                    {resume && (
                      <button
                        type="button"
                        onClick={() => setSelectedFile(null)}
                        className="btn-secondary btn"
                        disabled={uploading}
                      >
                        Cancel
                      </button>
                    )}
                    <button
                      type="button"
                      onClick={handleUploadSubmit}
                      disabled={uploading}
                      className="btn-primary btn"
                    >
                      {uploading ? <LoadingSpinner size="sm" /> : <Upload className="h-4 w-4" />}
                      {uploading ? 'Extracting Text...' : 'Upload & Extract Text'}
                    </button>
                  </div>
                )}
              </div>
            )}

            {/* Empty State when no resume uploaded yet */}
            {!resume && !selectedFile && (
              <EmptyState
                icon={<FileText />}
                title="No resume uploaded yet"
                description="Upload your resume in PDF format to receive instant AI feedback and ATS compatibility scoring."
                action={
                  <button
                    onClick={() => fileInputRef.current?.click()}
                    className="btn-primary btn btn-sm"
                  >
                    <Upload className="h-3.5 w-3.5" />
                    Upload Resume
                  </button>
                }
              />
            )}

            {/* Active Resume & AI Analysis Display */}
            {resume && !selectedFile && (
              <div className="space-y-6">
                {/* Resume Overview Header Card */}
                <div className="card p-6 space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-surface-100 pb-4">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-lg bg-primary-50 flex items-center justify-center flex-shrink-0">
                        <FileText className="h-5 w-5 text-primary-600" />
                      </div>
                      <div className="min-w-0">
                        <h3 className="font-semibold text-surface-900 truncate">
                          {resume.originalFileName}
                        </h3>
                        <p className="text-xs text-surface-400">
                          Uploaded on {new Date(resume.uploadedAt).toLocaleDateString()} &bull; {resume.fileSizeFormatted}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleAnalyzeResume}
                        disabled={analyzing}
                        className="btn-primary btn btn-sm"
                      >
                        {analyzing ? (
                          <LoadingSpinner size="sm" />
                        ) : (
                          <Sparkles className="h-3.5 w-3.5" />
                        )}
                        {analyzing ? 'Analyzing with AI...' : analysis ? 'Re-analyze' : 'Analyze Resume'}
                      </button>
                      <button
                        onClick={() => fileInputRef.current?.click()}
                        className="btn-secondary btn btn-sm"
                        disabled={analyzing}
                      >
                        <RefreshCw className="h-3.5 w-3.5" />
                        Replace
                      </button>
                      <button
                        onClick={() => setShowDeleteModal(true)}
                        className="btn-secondary btn btn-sm text-danger-600 hover:bg-danger-50 hover:border-danger-200"
                        disabled={analyzing}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  {/* Extracted Text Toggle Bar */}
                  <div className="flex items-center justify-between text-xs text-surface-500 pt-1">
                    <div className="flex items-center gap-2">
                      <CheckCircle2 className="h-3.5 w-3.5 text-success-600" />
                      <span>Extracted {resume.extractedTextLength.toLocaleString()} characters with PDFBox</span>
                    </div>
                    <button
                      onClick={() => setShowExtractedText(prev => !prev)}
                      className="text-primary-600 hover:underline flex items-center gap-1 font-medium"
                    >
                      {showExtractedText ? <EyeOff className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                      {showExtractedText ? 'Hide Extracted Text' : 'View Extracted Text'}
                    </button>
                  </div>

                  {/* Expandable Extracted Text */}
                  {showExtractedText && (
                    <div className="mt-3 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-xs font-medium text-surface-600">Raw PDFBox Text</span>
                        <button
                          onClick={handleCopyText}
                          className="text-xs text-primary-600 hover:underline flex items-center gap-1"
                        >
                          {copied ? <Check className="h-3 w-3 text-success-600" /> : <Copy className="h-3 w-3" />}
                          {copied ? 'Copied' : 'Copy Text'}
                        </button>
                      </div>
                      <div className="bg-surface-900 text-surface-100 rounded-lg p-4 font-mono text-xs overflow-x-auto max-h-60 leading-relaxed whitespace-pre-wrap">
                        {resume.extractedText}
                      </div>
                    </div>
                  )}
                </div>

                {/* AI Analyzing Spinner Overlay State */}
                {analyzing && (
                  <div className="card p-12 text-center space-y-4 animate-fade-in">
                    <div className="w-16 h-16 rounded-full bg-primary-50 flex items-center justify-center mx-auto">
                      <Sparkles className="h-8 w-8 text-primary-600 animate-spin" />
                    </div>
                    <h3 className="text-lg font-bold text-surface-900">
                      Analyzing your resume with Google Gemini AI...
                    </h3>
                    <p className="text-sm text-surface-500 max-w-md mx-auto">
                      Evaluating ATS formatting, technical skills, project relevance, and target role compatibility. This usually takes 3 to 5 seconds.
                    </p>
                  </div>
                )}

                {/* Stored Analysis Call to Action if not analyzed yet */}
                {!analysis && !analyzing && (
                  <div className="card p-8 text-center space-y-4 bg-gradient-to-b from-white to-primary-50/30 border-primary-100">
                    <div className="w-12 h-12 rounded-xl bg-primary-100 text-primary-600 flex items-center justify-center mx-auto">
                      <Sparkles className="h-6 w-6" />
                    </div>
                    <div>
                      <h3 className="text-base font-bold text-surface-900">
                        Ready for AI Resume &amp; ATS Scoring
                      </h3>
                      <p className="text-sm text-surface-500 max-w-md mx-auto mt-1">
                        Click &quot;Analyze Resume&quot; to get an AI-powered score, strengths, weaknesses, and skill recommendations.
                      </p>
                    </div>
                    <button
                      onClick={handleAnalyzeResume}
                      className="btn-primary btn btn-lg"
                    >
                      <Sparkles className="h-4 w-4" />
                      Analyze Resume with AI
                    </button>
                  </div>
                )}

                {/* FULL AI ANALYSIS RESULTS VIEW */}
                {analysis && !analyzing && (
                  <div className="space-y-6">
                    {/* Score Cards Row */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Overall Score */}
                      <div className="card p-6 flex items-center justify-between border-l-4 border-l-primary-600">
                        <div className="space-y-1">
                          <span className="text-xs font-semibold uppercase tracking-wider text-surface-400">
                            AI-Based Resume Score
                          </span>
                          <h4 className="text-2xl font-bold text-surface-900">
                            {analysis.overallScore} <span className="text-sm font-normal text-surface-400">/ 100</span>
                          </h4>
                          <p className="text-xs text-surface-500">
                            Overall quality, clarity, and impact evaluation
                          </p>
                        </div>
                        <div className={`w-16 h-16 rounded-full border-4 flex items-center justify-center text-lg font-extrabold ${getScoreColorClass(analysis.overallScore)}`}>
                          {analysis.overallScore}%
                        </div>
                      </div>

                      {/* ATS Score */}
                      <div className="card p-6 flex items-center justify-between border-l-4 border-l-purple-600">
                        <div className="space-y-1">
                          <span className="text-xs font-semibold uppercase tracking-wider text-surface-400">
                            AI-Based ATS Compatibility Score
                          </span>
                          <h4 className="text-2xl font-bold text-surface-900">
                            {analysis.atsScore} <span className="text-sm font-normal text-surface-400">/ 100</span>
                          </h4>
                          <p className="text-xs text-surface-500">
                            Target role: <span className="font-semibold text-surface-700">{analysis.targetRole || 'Software Developer'}</span>
                          </p>
                        </div>
                        <div className={`w-16 h-16 rounded-full border-4 flex items-center justify-center text-lg font-extrabold ${getScoreColorClass(analysis.atsScore)}`}>
                          {analysis.atsScore}%
                        </div>
                      </div>
                    </div>

                    {/* AI Executive Summary Card */}
                    {analysis.summary && (
                      <div className="card p-6 space-y-2">
                        <div className="flex items-center gap-2 text-primary-600">
                          <Sparkles className="h-4 w-4" />
                          <h3 className="text-sm font-semibold text-surface-800">AI Executive Summary</h3>
                        </div>
                        <p className="text-sm text-surface-600 leading-relaxed">
                          {analysis.summary}
                        </p>
                        <p className="text-xs text-surface-400 pt-2 border-t border-surface-100">
                          Analyzed on {new Date(analysis.analyzedAt).toLocaleDateString()} at{' '}
                          {new Date(analysis.analyzedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                    )}

                    {/* Strengths & Weaknesses */}
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      {/* Strengths */}
                      <div className="card p-6 space-y-3">
                        <div className="flex items-center gap-2 text-success-700">
                          <CheckCircle className="h-4 w-4 text-success-600" />
                          <h3 className="text-sm font-semibold">Resume Strengths</h3>
                        </div>
                        <ul className="space-y-2">
                          {(analysis.strengths || []).map((str, idx) => (
                            <li key={idx} className="flex items-start gap-2 text-xs sm:text-sm text-surface-700 bg-success-50/50 p-2.5 rounded-lg border border-success-100">
                              <span className="text-success-600 font-bold">•</span>
                              <span>{str}</span>
                            </li>
                          ))}
                        </ul>
                      </div>

                      {/* Weaknesses */}
                      <div className="card p-6 space-y-3">
                        <div className="flex items-center gap-2 text-amber-700">
                          <AlertTriangle className="h-4 w-4 text-amber-600" />
                          <h3 className="text-sm font-semibold">Areas for Improvement</h3>
                        </div>
                        <ul className="space-y-2">
                          {(analysis.weaknesses || []).map((weak, idx) => (
                            <li key={idx} className="flex items-start gap-2 text-xs sm:text-sm text-surface-700 bg-amber-50/50 p-2.5 rounded-lg border border-amber-100">
                              <span className="text-amber-600 font-bold">•</span>
                              <span>{weak}</span>
                            </li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    {/* Detected Skills by Category */}
                    <div className="card p-6 space-y-4">
                      <div className="flex items-center gap-2 text-primary-600">
                        <Zap className="h-4 w-4" />
                        <h3 className="text-sm font-semibold text-surface-800">Detected Skills in Resume</h3>
                      </div>

                      {Object.keys(groupedSkills).length > 0 ? (
                        <div className="space-y-4">
                          {Object.entries(groupedSkills).map(([category, skills]) => (
                            <div key={category} className="space-y-2">
                              <span className="text-xs font-semibold uppercase tracking-wider text-surface-400">
                                {category}
                              </span>
                              <div className="flex flex-wrap gap-2">
                                {skills.map((skill, sIdx) => (
                                  <span key={sIdx} className="badge-primary text-xs py-1 px-2.5 flex items-center gap-1.5">
                                    <span>{skill.name}</span>
                                    {skill.confidence && (
                                      <span className="text-[10px] opacity-75">
                                        ({Math.round(skill.confidence * 100)}%)
                                      </span>
                                    )}
                                  </span>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-xs text-surface-400">No skills detected.</p>
                      )}
                    </div>

                    {/* Missing Skills for Target Role */}
                    <div className="card p-6 space-y-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 text-danger-600">
                          <Target className="h-4 w-4" />
                          <h3 className="text-sm font-semibold text-surface-800">
                            Recommended / Missing Skills for {analysis.targetRole || 'Target Role'}
                          </h3>
                        </div>
                        <span className="text-xs text-surface-400">Based on industry standards</span>
                      </div>

                      {(analysis.missingSkills || []).length > 0 ? (
                        <div className="space-y-2">
                          {analysis.missingSkills.map((missing, mIdx) => (
                            <div key={mIdx} className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 p-3 rounded-lg bg-surface-50 border border-surface-200">
                              <div className="space-y-0.5">
                                <span className="font-semibold text-sm text-surface-800">{missing.name}</span>
                                <p className="text-xs text-surface-500">{missing.reason}</p>
                              </div>
                              <span className={getPriorityBadgeClass(missing.priority)}>
                                {missing.priority} PRIORITY
                              </span>
                            </div>
                          ))}
                        </div>
                      ) : (
                        <p className="text-xs text-surface-400">No major missing skills identified for target role!</p>
                      )}
                    </div>

                    {/* Actionable Suggestions */}
                    <div className="card p-6 space-y-3">
                      <div className="flex items-center gap-2 text-indigo-600">
                        <Lightbulb className="h-4 w-4" />
                        <h3 className="text-sm font-semibold text-surface-800">Actionable Suggestions</h3>
                      </div>
                      <div className="space-y-2">
                        {(analysis.suggestions || []).map((sug, sIdx) => (
                          <div key={sIdx} className="flex items-start gap-2 text-xs sm:text-sm text-surface-700 p-2.5 rounded-lg bg-indigo-50/30 border border-indigo-100">
                            <span className="text-indigo-600 font-bold">•</span>
                            <span>{sug}</span>
                          </div>
                        ))}
                      </div>
                    </div>

                    {/* Section Analysis Tabs */}
                    {analysis.sectionAnalysis && (
                      <div className="card p-6 space-y-4">
                        <div className="flex items-center gap-2 text-surface-700">
                          <Layers className="h-4 w-4 text-primary-600" />
                          <h3 className="text-sm font-semibold">Section-by-Section Feedback</h3>
                        </div>

                        {/* Tabs Bar */}
                        <div className="flex overflow-x-auto gap-1 border-b border-surface-200 pb-2 scrollbar-none">
                          {['summary', 'education', 'skills', 'projects', 'experience', 'certifications'].map((tab) => (
                            <button
                              key={tab}
                              onClick={() => setActiveSectionTab(tab)}
                              className={`px-3 py-1.5 text-xs font-medium rounded-lg capitalize whitespace-nowrap transition-all ${
                                activeSectionTab === tab
                                  ? 'bg-primary-600 text-white'
                                  : 'text-surface-600 hover:bg-surface-100'
                              }`}
                            >
                              {tab}
                            </button>
                          ))}
                        </div>

                        {/* Selected Tab Feedback */}
                        <div className="p-4 bg-surface-50 rounded-lg border border-surface-200">
                          <p className="text-xs font-semibold text-surface-500 uppercase tracking-wider mb-1">
                            {activeSectionTab} Feedback
                          </p>
                          <p className="text-sm text-surface-700 leading-relaxed">
                            {analysis.sectionAnalysis[activeSectionTab] || 'No specific feedback for this section.'}
                          </p>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            )}
          </>
        )}

        {/* Delete Confirmation Modal */}
        {showDeleteModal && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-surface-900/50 backdrop-blur-sm animate-fade-in">
            <div className="card max-w-md w-full p-6 space-y-4 shadow-modal">
              <div className="flex items-center gap-3 text-danger-600">
                <AlertTriangle className="h-6 w-6 flex-shrink-0" />
                <h3 className="font-bold text-lg text-surface-900">Delete Resume</h3>
              </div>
              <p className="text-sm text-surface-600 leading-relaxed">
                Are you sure you want to delete <span className="font-semibold text-surface-800">{resume?.originalFileName}</span>?
                This will permanently remove the extracted text and AI analysis data from the server.
              </p>
              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => setShowDeleteModal(false)}
                  className="btn-secondary btn"
                  disabled={deleting}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleDeleteResume}
                  className="btn-danger btn"
                  disabled={deleting}
                >
                  {deleting ? <LoadingSpinner size="sm" /> : <Trash2 className="h-4 w-4" />}
                  {deleting ? 'Deleting...' : 'Yes, Delete'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </AppLayout>
  )
}
