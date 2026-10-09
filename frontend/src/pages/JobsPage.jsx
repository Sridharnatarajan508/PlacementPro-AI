import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import JobFilters from '../components/jobs/JobFilters'
import JobCard from '../components/jobs/JobCard'
import SavedJobCard from '../components/jobs/SavedJobCard'
import JobMatchDetails from '../components/jobs/JobMatchDetails'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import EmptyState from '../components/ui/EmptyState'
import { jobService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import { Sparkles, Briefcase, Bookmark, RefreshCw, Search } from 'lucide-react'

export default function JobsPage() {
  const [activeTab, setActiveTab] = useState('recommended') // 'recommended' | 'all' | 'saved'

  // Data states
  const [allJobs, setAllJobs] = useState([])
  const [recommendedJobs, setRecommendedJobs] = useState([])
  const [savedJobs, setSavedJobs] = useState([])

  // Loading states
  const [loadingAll, setLoadingAll] = useState(false)
  const [loadingRec, setLoadingRec] = useState(false)
  const [loadingSaved, setLoadingSaved] = useState(false)
  const [analyzingJobId, setAnalyzingJobId] = useState(null)

  // Filters for 'all' tab
  const [filters, setFilters] = useState({
    keyword: '',
    location: '',
    jobType: 'ALL',
    experienceLevel: 'ALL',
    minimumMatchScore: null,
  })

  // Modal states for Match Details
  const [selectedJob, setSelectedJob] = useState(null)
  const [selectedMatch, setSelectedMatch] = useState(null)
  const [loadingModalMatch, setLoadingModalMatch] = useState(false)

  // Fetch Recommended Jobs
  const fetchRecommended = useCallback(async () => {
    setLoadingRec(true)
    try {
      const { data } = await jobService.getRecommendedJobs()
      setRecommendedJobs(data || [])
    } catch (err) {
      logError(err)
      toast.error(getErrorMessage(err) || 'Failed to load recommended jobs')
    } finally {
      setLoadingRec(false)
    }
  }, [])

  // Fetch All Jobs
  const fetchAllJobs = useCallback(async (currentFilters = filters) => {
    setLoadingAll(true)
    try {
      const params = {}
      if (currentFilters.keyword) params.keyword = currentFilters.keyword
      if (currentFilters.location) params.location = currentFilters.location
      if (currentFilters.jobType && currentFilters.jobType !== 'ALL') params.jobType = currentFilters.jobType
      if (currentFilters.experienceLevel && currentFilters.experienceLevel !== 'ALL') params.experienceLevel = currentFilters.experienceLevel
      if (currentFilters.minimumMatchScore) params.minimumMatchScore = currentFilters.minimumMatchScore

      const { data } = await jobService.getJobs(params)
      setAllJobs(data || [])
    } catch (err) {
      logError(err)
      toast.error(getErrorMessage(err) || 'Failed to search jobs')
    } finally {
      setLoadingAll(false)
    }
  }, [filters])

  // Fetch Saved Jobs
  const fetchSaved = useCallback(async () => {
    setLoadingSaved(true)
    try {
      const { data } = await jobService.getSavedJobs()
      setSavedJobs(data || [])
    } catch (err) {
      logError(err)
      toast.error(getErrorMessage(err) || 'Failed to load saved jobs')
    } finally {
      setLoadingSaved(false)
    }
  }, [])

  function logError(e) {
    console.error('Job operation error:', e)
  }

  // Initial load
  useEffect(() => {
    fetchRecommended()
    fetchAllJobs()
    fetchSaved()
  }, [])

  // Handle filter changes
  const handleApplyFilters = () => {
    fetchAllJobs(filters)
  }

  const handleResetFilters = () => {
    const clean = { keyword: '', location: '', jobType: 'ALL', experienceLevel: 'ALL', minimumMatchScore: null }
    setFilters(clean)
    fetchAllJobs(clean)
  }

  // Handle Save / Unsave toggle
  const handleSaveToggle = async (jobId, isCurrentlySaved) => {
    try {
      if (isCurrentlySaved) {
        await jobService.unsaveJob(jobId)
        toast.success('Job removed from saved list')
      } else {
        await jobService.saveJob(jobId)
        toast.success('Job saved to your bookmarks!')
      }

      // Update state in all views
      const updateList = (list) =>
        list.map((j) => (j.id === jobId ? { ...j, isSaved: !isCurrentlySaved } : j))

      setAllJobs(prev => updateList(prev))
      setRecommendedJobs(prev =>
        prev.map(r => (r.job.id === jobId ? { ...r, job: { ...r.job, isSaved: !isCurrentlySaved } } : r))
      )

      if (isCurrentlySaved) {
        setSavedJobs(prev => prev.filter(j => j.id !== jobId))
      } else {
        fetchSaved()
      }
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  // Handle AI Match Analysis
  const handleAnalyzeMatch = async (jobId) => {
    setAnalyzingJobId(jobId)
    setLoadingModalMatch(true)

    // Find job details from state
    let targetJob = allJobs.find(j => j.id === jobId)
    if (!targetJob) {
      const rec = recommendedJobs.find(r => r.job.id === jobId)
      if (rec) targetJob = rec.job
    }
    if (!targetJob) {
      targetJob = savedJobs.find(j => j.id === jobId)
    }

    setSelectedJob(targetJob)
    setSelectedMatch(null)

    try {
      const { data: matchResult } = await jobService.matchJob(jobId)
      setSelectedMatch(matchResult)
      toast.success(`Match analyzed: ${matchResult.overallScore}% score!`)

      // Update job match score in local lists
      const updateMatchInJob = (j) =>
        j.id === jobId ? { ...j, matchScore: matchResult.overallScore, matchRecommendation: matchResult.recommendation } : j

      setAllJobs(prev => prev.map(updateMatchInJob))
      setSavedJobs(prev => prev.map(updateMatchInJob))
      setRecommendedJobs(prev =>
        prev.map(r =>
          r.job.id === jobId
            ? { ...r, job: updateMatchInJob(r.job), match: matchResult }
            : r
        )
      )
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setAnalyzingJobId(null)
      setLoadingModalMatch(false)
    }
  }

  // View Details Modal
  const handleViewDetails = async (job) => {
    setSelectedJob(job)
    setSelectedMatch(null)

    if (job.matchScore != null) {
      setLoadingModalMatch(true)
      try {
        const { data } = await jobService.getMatchForJob(job.id)
        setSelectedMatch(data)
      } catch (e) {
        // Match may not exist yet
      } finally {
        setLoadingModalMatch(false)
      }
    }
  }

  return (
    <AppLayout>
      <div className="space-y-6 animate-slide-up">
        {/* Header Banner */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-surface-900 flex items-center gap-2">
              <Sparkles className="h-6 w-6 text-primary-600" />
              AI Job Matcher
            </h1>
            <p className="text-sm text-surface-500 mt-1">
              Find jobs that match your skills, resume and career goals.
            </p>
          </div>

          {/* Tab Navigation */}
          <div className="flex items-center bg-surface-200/70 p-1 rounded-xl self-start sm:self-auto border border-surface-200">
            <button
              onClick={() => setActiveTab('recommended')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                activeTab === 'recommended'
                  ? 'bg-white text-primary-700 shadow-xs'
                  : 'text-surface-600 hover:text-surface-900'
              }`}
            >
              <Sparkles className="h-3.5 w-3.5 text-primary-600" />
              <span>Recommended</span>
              {recommendedJobs.length > 0 && (
                <span className="bg-primary-100 text-primary-700 px-1.5 py-0.2 rounded-full text-[10px] font-bold">
                  {recommendedJobs.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('all')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                activeTab === 'all'
                  ? 'bg-white text-primary-700 shadow-xs'
                  : 'text-surface-600 hover:text-surface-900'
              }`}
            >
              <Briefcase className="h-3.5 w-3.5" />
              <span>All Jobs</span>
              {allJobs.length > 0 && (
                <span className="bg-surface-200 text-surface-700 px-1.5 py-0.2 rounded-full text-[10px] font-bold">
                  {allJobs.length}
                </span>
              )}
            </button>

            <button
              onClick={() => setActiveTab('saved')}
              className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold flex items-center gap-1.5 transition ${
                activeTab === 'saved'
                  ? 'bg-white text-primary-700 shadow-xs'
                  : 'text-surface-600 hover:text-surface-900'
              }`}
            >
              <Bookmark className="h-3.5 w-3.5" />
              <span>Saved</span>
              {savedJobs.length > 0 && (
                <span className="bg-surface-200 text-surface-700 px-1.5 py-0.2 rounded-full text-[10px] font-bold">
                  {savedJobs.length}
                </span>
              )}
            </button>
          </div>
        </div>

        {/* Tab 1: Recommended Jobs */}
        {activeTab === 'recommended' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-surface-800 uppercase tracking-wider">
                Top AI Matches For You
              </h2>
              <button
                onClick={fetchRecommended}
                className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1 hover:underline"
              >
                <RefreshCw className="h-3 w-3" />
                Refresh Recommendations
              </button>
            </div>

            {loadingRec ? (
              <LoadingSpinner text="Analyzing and ranking jobs for your profile..." />
            ) : recommendedJobs.length === 0 ? (
              <EmptyState
                icon={<Sparkles className="text-primary-500" />}
                title="No recommendations found"
                description="Upload your resume or update your profile skills to get personalized AI job matches."
                action={
                  <button onClick={() => setActiveTab('all')} className="btn-primary btn btn-sm">
                    Browse All Jobs
                  </button>
                }
              />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {recommendedJobs.map(({ job, match }) => (
                  <JobCard
                    key={job.id}
                    job={job}
                    onSaveToggle={handleSaveToggle}
                    onViewDetails={handleViewDetails}
                    onAnalyze={handleAnalyzeMatch}
                    analyzingId={analyzingJobId}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 2: All Jobs */}
        {activeTab === 'all' && (
          <div className="space-y-4">
            <JobFilters
              filters={filters}
              setFilters={setFilters}
              onApply={handleApplyFilters}
              onReset={handleResetFilters}
            />

            <div className="flex items-center justify-between pt-1">
              <h2 className="text-xs font-bold text-surface-500 uppercase tracking-wider">
                Showing {allJobs.length} Job{allJobs.length === 1 ? '' : 's'}
              </h2>
            </div>

            {loadingAll ? (
              <LoadingSpinner text="Searching jobs..." />
            ) : allJobs.length === 0 ? (
              <EmptyState
                icon={<Search className="text-surface-400" />}
                title="No jobs match your filters"
                description="Try clearing search terms or selecting 'All' for job types and experience levels."
                action={
                  <button onClick={handleResetFilters} className="btn-secondary btn btn-sm">
                    Clear Filters
                  </button>
                }
              />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {allJobs.map((job) => (
                  <JobCard
                    key={job.id}
                    job={job}
                    onSaveToggle={handleSaveToggle}
                    onViewDetails={handleViewDetails}
                    onAnalyze={handleAnalyzeMatch}
                    analyzingId={analyzingJobId}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* Tab 3: Saved Jobs */}
        {activeTab === 'saved' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-surface-800 uppercase tracking-wider">
                Saved Bookmarks ({savedJobs.length})
              </h2>
            </div>

            {loadingSaved ? (
              <LoadingSpinner text="Loading saved jobs..." />
            ) : savedJobs.length === 0 ? (
              <EmptyState
                icon={<Bookmark className="text-surface-400" />}
                title="You haven't saved any jobs yet"
                description="Click the bookmark icon on any job card to save it for later review."
                action={
                  <button onClick={() => setActiveTab('all')} className="btn-primary btn btn-sm">
                    Find Jobs
                  </button>
                }
              />
            ) : (
              <div className="space-y-3">
                {savedJobs.map((job) => (
                  <SavedJobCard
                    key={job.id}
                    job={job}
                    onUnsave={(id) => handleSaveToggle(id, true)}
                    onViewDetails={handleViewDetails}
                    onAnalyze={handleAnalyzeMatch}
                  />
                ))}
              </div>
            )}
          </div>
        )}

        {/* Job Match Details Modal */}
        {selectedJob && (
          <JobMatchDetails
            job={selectedJob}
            match={selectedMatch}
            loading={loadingModalMatch || analyzingJobId === selectedJob.id}
            onClose={() => {
              setSelectedJob(null)
              setSelectedMatch(null)
            }}
            onAnalyze={handleAnalyzeMatch}
          />
        )}
      </div>
    </AppLayout>
  )
}
