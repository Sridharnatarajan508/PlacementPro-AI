import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { profileService, jobService, skillGapService, careerRoadmapService, interviewService, jobPreparationService, careerAnalyticsService, jobApplicationService } from '../services/api'
import AppLayout from '../components/layout/AppLayout'
import EmptyState from '../components/ui/EmptyState'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import {
  FileText, Briefcase, Zap, Map, Mic, ClipboardList,
  ArrowRight, TrendingUp, User, BookOpen, Sparkles, Building2, MapPin, CheckCircle2, Clock, CheckSquare, Award,
  MessageSquare, Bot, LineChart, Target, Calendar, PhoneCall
} from 'lucide-react'

const NEXT_STEPS = [
  {
    id: 'resume',
    title: 'Upload your resume',
    description: 'Get an AI-powered ATS score and improvement suggestions.',
    icon: FileText,
    to: '/resume',
    cta: 'Upload Resume',
    color: 'bg-blue-50 text-blue-600',
  },
  {
    id: 'jobs',
    title: 'Explore target jobs',
    description: 'Find matching jobs and get an AI compatibility score.',
    icon: Briefcase,
    to: '/jobs',
    cta: 'View Jobs',
    color: 'bg-purple-50 text-purple-600',
  },
  {
    id: 'applications',
    title: 'Track Job Applications',
    description: 'Manage your job search pipeline from Saved to Interview and Offer.',
    icon: ClipboardList,
    to: '/applications',
    cta: 'Application Tracker',
    color: 'bg-blue-50 text-blue-700',
  },
  {
    id: 'skillgap',
    title: 'Analyze your skill gap',
    description: 'Discover missing skills and get a personalized learning plan.',
    icon: Zap,
    to: '/skill-gap',
    cta: 'Analyze Gap',
    color: 'bg-amber-50 text-amber-600',
  },
  {
    id: 'roadmap',
    title: 'AI Career Roadmap',
    description: 'Follow your week-by-week personalized learning curriculum.',
    icon: Map,
    to: '/career-roadmap',
    cta: 'Open Roadmap',
    color: 'bg-green-50 text-green-600',
  },
  {
    id: 'preparation',
    title: 'AI Job Preparation',
    description: 'Follow a customized 7–14 day plan for your targeted job opening.',
    icon: BookOpen,
    to: '/preparation',
    cta: 'Start Prep',
    color: 'bg-indigo-50 text-indigo-600',
  },
  {
    id: 'mockinterview',
    title: 'AI Mock Interview',
    description: 'Practice role-specific interview questions with instant AI scoring.',
    icon: Mic,
    to: '/mock-interview',
    cta: 'Practice Now',
    color: 'bg-rose-50 text-rose-600',
  },
  {
    id: 'assistant',
    title: 'AI Career Assistant',
    description: 'Ask personalized career strategy, interview tips, and questions.',
    icon: MessageSquare,
    to: '/ai-assistant',
    cta: 'Ask Assistant',
    color: 'bg-purple-50 text-purple-600',
  },
]

export default function DashboardPage() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [recommendedJobs, setRecommendedJobs] = useState([])
  const [latestSkillGap, setLatestSkillGap] = useState(null)
  const [latestRoadmap, setLatestRoadmap] = useState(null)
  const [interviewHistory, setInterviewHistory] = useState([])
  const [preparations, setPreparations] = useState([])
  const [careerAnalytics, setCareerAnalytics] = useState(null)
  const [appStats, setAppStats] = useState(null)
  const [upcomingTasks, setUpcomingTasks] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    Promise.allSettled([
      profileService.getProfile().then(({ data }) => setProfile(data)),
      jobService.getRecommendedJobs().then(({ data }) => setRecommendedJobs((data || []).slice(0, 3))),
      skillGapService.getLatestSkillGap().then(({ data }) => setLatestSkillGap(data)),
      careerRoadmapService.getLatestRoadmap().then(({ data }) => setLatestRoadmap(data)),
      interviewService.getInterviewHistory().then(({ data }) => setInterviewHistory(data || [])),
      jobPreparationService.getAll().then(({ data }) => setPreparations(data || [])),
      careerAnalyticsService.getAnalytics().then(({ data }) => setCareerAnalytics(data)).catch(() => {}),
      jobApplicationService.getStats().then(({ data }) => setAppStats(data)).catch(() => {}),
      jobApplicationService.getUpcoming().then(({ data }) => setUpcomingTasks(data)).catch(() => {})
    ]).finally(() => setLoading(false))
  }, [])

  const firstName = user?.name?.split(' ')[0] || 'there'
  const completedInterviews = interviewHistory.filter(i => i.status === 'COMPLETED')
  const latestInterview = completedInterviews.length > 0 ? completedInterviews[0] : (interviewHistory.length > 0 ? interviewHistory[0] : null)
  const bestScore = completedInterviews.length > 0 ? Math.max(...completedInterviews.map(i => i.overallScore || 0)) : null

  return (
    <AppLayout>
      {loading ? (
        <LoadingSpinner text="Loading your dashboard..." />
      ) : (
        <div className="space-y-6 animate-slide-up">
          {/* Welcome */}
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
            <div>
              <h2 className="text-2xl font-bold text-surface-900">
                Welcome back, {firstName}! 👋
              </h2>
              <p className="text-surface-500 text-sm mt-1">
                {profile?.targetRole
                  ? `Target role: ${profile.targetRole}`
                  : 'Set your target role to get personalized recommendations.'}
              </p>
            </div>
            {!profile?.onboardingComplete && (
              <Link to="/onboarding" className="btn-primary btn btn-sm flex-shrink-0">
                Complete Profile
                <ArrowRight className="h-3.5 w-3.5" />
              </Link>
            )}
          </div>

          {/* Career Intelligence Widget */}
          <div className="card p-6 border border-indigo-100 bg-gradient-to-r from-indigo-50/40 via-white to-purple-50/30">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <div className="p-1.5 bg-indigo-100/70 text-indigo-700 rounded-lg">
                  <Sparkles className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="section-title mb-0">AI Career Intelligence</h3>
                  <p className="text-xs text-surface-500">Holistic readiness evaluation across all preparation areas</p>
                </div>
              </div>
              <Link to="/career-analytics" className="text-xs font-semibold text-indigo-600 hover:text-indigo-700 flex items-center gap-1 hover:underline">
                View full intelligence
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>

            {careerAnalytics ? (
              <div className="grid grid-cols-1 md:grid-cols-12 gap-5 items-center">
                <div className="md:col-span-3 flex items-center gap-3.5">
                  <div className="w-14 h-14 rounded-2xl bg-indigo-600 text-white flex flex-col items-center justify-center font-extrabold shadow-sm flex-shrink-0">
                    <span className="text-xl leading-none">{careerAnalytics.overallReadinessScore || 0}%</span>
                    <span className="text-[9px] uppercase tracking-wider opacity-90 mt-0.5">SCORE</span>
                  </div>
                  <div>
                    <span className="inline-block text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 bg-indigo-50 text-indigo-700 rounded-md border border-indigo-200">
                      {careerAnalytics.readinessLevel || 'NEEDS PREPARATION'}
                    </span>
                    <p className="text-xs text-surface-600 font-medium mt-1">Overall Career Readiness</p>
                  </div>
                </div>

                <div className="md:col-span-6 space-y-1 text-xs">
                  {careerAnalytics.weaknesses && careerAnalytics.weaknesses.length > 0 && (
                    <div className="flex items-start gap-1.5 text-surface-700">
                      <span className="font-bold text-rose-700 flex-shrink-0">Top Priority Gap:</span>
                      <span className="truncate">{careerAnalytics.weaknesses[0].skill} — {careerAnalytics.weaknesses[0].recommendedAction}</span>
                    </div>
                  )}
                  {careerAnalytics.recommendations && careerAnalytics.recommendations.length > 0 && (
                    <div className="flex items-start gap-1.5 text-surface-700">
                      <span className="font-bold text-indigo-700 flex-shrink-0">Next Action:</span>
                      <span className="truncate">{careerAnalytics.recommendations[0].action}</span>
                    </div>
                  )}
                </div>

                <div className="md:col-span-3 flex flex-wrap justify-start md:justify-end gap-2">
                  <Link to="/ai-assistant?prompt=What should I focus on next to maximize my placement readiness?" className="btn-primary btn btn-sm inline-flex items-center gap-1.5 shadow-xs">
                    <Bot className="h-3.5 w-3.5" />
                    <span>Ask AI Assistant</span>
                  </Link>
                  <Link to="/career-analytics" className="btn-secondary btn btn-sm inline-flex items-center gap-1.5">
                    <TrendingUp className="h-3.5 w-3.5" />
                    <span>Analytics</span>
                  </Link>
                </div>
              </div>
            ) : (
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 py-2">
                <div>
                  <p className="text-sm font-semibold text-surface-900">Career Intelligence Dashboard Ready</p>
                  <p className="text-xs text-surface-500 mt-0.5">Generate a consolidated analysis of your complete career readiness, interview scores, and target goals.</p>
                </div>
                <Link to="/career-analytics" className="btn-primary btn btn-sm flex-shrink-0">
                  <span>Explore Intelligence</span>
                  <ArrowRight className="h-3.5 w-3.5" />
                </Link>
              </div>
            )}
          </div>

          {/* Summary cards */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            {[
              { label: 'Resume ATS Score', value: '—', sub: 'Upload a resume', icon: FileText, to: '/resume', color: 'text-blue-600' },
              {
                label: 'Skill Readiness',
                value: latestSkillGap ? `${latestSkillGap.readinessScore}%` : '—',
                sub: latestSkillGap ? `${latestSkillGap.targetRole}` : 'Analyze skill gap',
                icon: Zap,
                to: '/skill-gap',
                color: 'text-amber-600'
              },
              { label: 'Recommended Jobs', value: recommendedJobs.length ? `${recommendedJobs.length} Live` : '—', sub: 'Explore matching jobs', icon: Briefcase, to: '/jobs', color: 'text-purple-600' },
              {
                label: 'Interview Score',
                value: latestInterview?.overallScore !== undefined && latestInterview?.overallScore !== null ? `${latestInterview.overallScore}%` : '—',
                sub: completedInterviews.length > 0 ? `${completedInterviews.length} Completed (Best: ${bestScore}%)` : 'Take a mock interview',
                icon: Mic,
                to: '/mock-interview',
                color: 'text-rose-600'
              },
            ].map(({ label, value, sub, icon: Icon, to, color }) => (
              <Link key={label} to={to} className="card-hover p-4 flex flex-col gap-2 cursor-pointer">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-medium text-surface-500">{label}</span>
                  <Icon className={`h-4 w-4 ${color}`} />
                </div>
                <p className="text-2xl font-bold text-surface-900 truncate">{value}</p>
                <p className="text-xs text-surface-400 truncate">{sub}</p>
              </Link>
            ))}
          </div>

          {/* Career Readiness */}
          <div className="card p-6">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-primary-600" />
                <h3 className="section-title mb-0">Career Readiness Assessment</h3>
              </div>
              {latestSkillGap && (
                <Link to="/skill-gap" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                  View full analysis
                  <ArrowRight className="h-3 w-3" />
                </Link>
              )}
            </div>

            {latestSkillGap ? (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-5 items-center">
                <div className="flex items-center gap-4">
                  <div className="w-16 h-16 rounded-2xl bg-amber-50 border-2 border-amber-300 text-amber-800 flex flex-col items-center justify-center font-bold shadow-xs">
                    <span className="text-2xl leading-none">{latestSkillGap.readinessScore}%</span>
                    <span className="text-[10px] opacity-80 mt-0.5">READY</span>
                  </div>
                  <div>
                    <h4 className="font-bold text-surface-900 text-sm">Target Role Fit</h4>
                    <p className="text-xs text-surface-600 font-medium">{latestSkillGap.targetRole}</p>
                    <p className="text-[11px] text-surface-400 mt-0.5">
                      {latestSkillGap.skillCoverageScore}% Essential Skills Covered
                    </p>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs font-medium">
                    <span className="text-surface-600">Skill Coverage</span>
                    <span className="text-surface-900 font-bold">{latestSkillGap.skillCoverageScore}%</span>
                  </div>
                  <div className="h-2.5 w-full bg-surface-100 rounded-full overflow-hidden">
                    <div
                      className="h-full bg-amber-500 rounded-full"
                      style={{ width: `${latestSkillGap.skillCoverageScore}%` }}
                    />
                  </div>
                  <p className="text-[11px] text-surface-400">
                    {latestSkillGap.skillGaps?.length || 0} skill gaps identified in learning roadmap
                  </p>
                </div>

                <div className="flex justify-start md:justify-end">
                  <Link to="/skill-gap" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                    <Zap className="h-3.5 w-3.5" />
                    <span>Open Skill Roadmap</span>
                  </Link>
                </div>
              </div>
            ) : (
              <EmptyState
                icon={<TrendingUp />}
                title="Your career readiness score will appear here"
                description="Analyze your target role or select a job vacancy to calculate your AI readiness score and generate a custom learning roadmap."
                action={
                  <Link to="/skill-gap" className="btn-primary btn btn-sm">
                    Analyze Skill Gap
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                }
              />
            )}
          </div>

          {/* Career Roadmap Progress */}
          <div className="card p-6 border border-surface-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Map className="h-5 w-5 text-primary-600" />
                <h3 className="section-title mb-0">Career Roadmap Progress</h3>
              </div>
              {latestRoadmap && (
                <Link to="/career-roadmap" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                  View full roadmap
                  <ArrowRight className="h-3 w-3" />
                </Link>
              )}
            </div>

            {latestRoadmap ? (
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 items-center">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="badge-primary text-xs font-semibold px-2.5 py-0.5 rounded-full">
                        {latestRoadmap.targetRole}
                      </span>
                      <span className="text-[11px] text-surface-500 font-medium">
                        {latestRoadmap.currentLevel || 'Entry Level'}
                      </span>
                    </div>
                    <p className="text-xs text-surface-500 line-clamp-2 mt-1">
                      {latestRoadmap.summary || latestRoadmap.aiSummary || 'Curated personalized learning path.'}
                    </p>
                  </div>

                  <div className="space-y-1.5">
                    <div className="flex justify-between text-xs font-medium">
                      <span className="text-surface-600">Readiness Score</span>
                      <span className="text-surface-900 font-bold">{latestRoadmap.overallReadinessScore || latestRoadmap.overallProgress || 0}%</span>
                    </div>
                    <div className="h-2.5 w-full bg-surface-100 rounded-full overflow-hidden">
                      <div
                        className="h-full bg-primary-600 rounded-full transition-all duration-300"
                        style={{ width: `${latestRoadmap.overallReadinessScore || latestRoadmap.overallProgress || 0}%` }}
                      />
                    </div>
                    <p className="text-[11px] text-surface-400">
                      Primary Focus: <strong className="text-surface-700">{latestRoadmap.recommendedSkills?.[0] || latestRoadmap.skillGaps?.[0] || 'Technical Mastery'}</strong>
                    </p>
                  </div>

                  <div className="flex justify-start md:justify-end">
                    <Link to="/career-roadmap" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                      <Map className="h-3.5 w-3.5" />
                      <span>View Career Roadmap</span>
                    </Link>
                  </div>
                </div>
              </div>
            ) : (
              <EmptyState
                icon={<Map />}
                title="Generate your personalized career roadmap"
                description="Get an AI-powered curriculum, milestones, capstone projects, and placement guidance tailored for your dream career."
                action={
                  <Link to="/career-roadmap" className="btn-primary btn btn-sm">
                    Generate Career Roadmap
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                }
              />
            )}
          </div>

          {/* AI Job Preparation Progress */}
          <div className="card p-6 border border-surface-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <BookOpen className="h-5 w-5 text-indigo-600" />
                <h3 className="section-title mb-0">AI Job Preparation</h3>
              </div>
              {preparations.length > 0 && (
                <Link to="/preparation" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                  View all preparations ({preparations.length})
                  <ArrowRight className="h-3 w-3" />
                </Link>
              )}
            </div>

            {preparations.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center">
                <div className="flex items-center gap-3">
                  <div className="w-14 h-14 rounded-2xl bg-indigo-50 border-2 border-indigo-200 text-indigo-800 flex flex-col items-center justify-center font-bold shadow-xs">
                    <span className="text-xl leading-none">{preparations[0].readinessScore || 0}%</span>
                    <span className="text-[9px] opacity-80 mt-0.5 uppercase">READY</span>
                  </div>
                  <div>
                    <h4 className="font-bold text-surface-900 text-xs">{preparations[0].jobTitle}</h4>
                    <p className="text-xs text-surface-600 font-medium">{preparations[0].company}</p>
                    <span className="text-[10px] text-surface-400">
                      {preparations[0].preparationLevel} • {preparations[0].location || 'Remote'}
                    </span>
                  </div>
                </div>

                <div className="space-y-1.5 md:col-span-2">
                  <div className="flex justify-between text-xs">
                    <span className="font-semibold text-surface-700">Next Preparation Focus</span>
                    <span className="text-primary-600 font-bold">Day 1</span>
                  </div>
                  <p className="text-xs text-surface-600 font-medium bg-surface-50 p-2.5 rounded-lg border border-surface-100 line-clamp-1">
                    🎯 {preparations[0].preparationPlan?.[0]?.title || preparations[0].technicalTopics?.[0] || 'Core Revision & Practice'}
                  </p>
                </div>

                <div className="flex justify-start md:justify-end">
                  <Link
                    to={`/preparation?jobId=${preparations[0].jobId}`}
                    className="btn-primary btn btn-sm inline-flex items-center gap-1.5"
                  >
                    <BookOpen className="h-3.5 w-3.5" />
                    <span>Continue Preparation</span>
                  </Link>
                </div>
              </div>
            ) : (
              <EmptyState
                icon={<BookOpen />}
                title="Choose a job and start preparing"
                description="Pick any matched job to get a customized 7–14 day preparation plan covering coding, technical questions, and interview topics."
                action={
                  <Link to="/jobs" className="btn-primary btn btn-sm">
                    Explore Jobs to Prepare
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                }
              />
            )}
          </div>

          {/* AI Mock Interview Performance & Readiness */}
          <div className="card p-6 border border-surface-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <Mic className="h-5 w-5 text-rose-600" />
                <h3 className="section-title mb-0">AI Mock Interview Performance</h3>
              </div>
              {completedInterviews.length > 0 && (
                <Link to="/mock-interview" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                  View interview history
                  <ArrowRight className="h-3 w-3" />
                </Link>
              )}
            </div>

            {completedInterviews.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-center">
                <div className="flex items-center gap-3">
                  <div className="w-14 h-14 rounded-2xl bg-rose-50 border-2 border-rose-300 text-rose-800 flex flex-col items-center justify-center font-bold shadow-xs">
                    <span className="text-xl leading-none">{latestInterview.overallScore || 0}%</span>
                    <span className="text-[9px] opacity-80 mt-0.5 uppercase">LATEST</span>
                  </div>
                  <div>
                    <h4 className="font-bold text-surface-900 text-xs">Latest Simulation</h4>
                    <p className="text-xs text-surface-600 font-medium">{latestInterview.targetRole}</p>
                    <span className="text-[10px] text-surface-400">
                      {latestInterview.interviewType} • {latestInterview.difficulty}
                    </span>
                  </div>
                </div>

                <div className="bg-surface-50 p-3 rounded-xl border border-surface-100 text-center">
                  <span className="text-[10px] font-bold text-surface-400 uppercase block">Best Score</span>
                  <span className="text-lg font-bold text-emerald-600">{bestScore || 0}%</span>
                  <span className="text-[10px] text-surface-400 block mt-0.5">Peak Performance</span>
                </div>

                <div className="bg-surface-50 p-3 rounded-xl border border-surface-100 text-center">
                  <span className="text-[10px] font-bold text-surface-400 uppercase block">Total Sessions</span>
                  <span className="text-lg font-bold text-surface-900">{completedInterviews.length}</span>
                  <span className="text-[10px] text-surface-400 block mt-0.5">Completed Simulations</span>
                </div>

                <div className="flex justify-start md:justify-end">
                  <Link to="/mock-interview" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                    <Mic className="h-3.5 w-3.5" />
                    <span>Practice Interview</span>
                  </Link>
                </div>
              </div>
            ) : (
              <EmptyState
                icon={<Mic />}
                title="No Mock Interviews Completed Yet"
                description="Simulate realistic behavioral, technical, and architecture interviews with real-time AI scoring and tailored feedback."
                action={
                  <Link to="/mock-interview" className="btn-primary btn btn-sm">
                    Start AI Mock Interview
                    <ArrowRight className="h-3.5 w-3.5" />
                  </Link>
                }
              />
            )}
          </div>

          {/* Job Application Tracker Widget */}
          <div className="card p-6 border border-surface-200">
            <div className="flex items-center justify-between mb-4">
              <div className="flex items-center gap-2">
                <ClipboardList className="h-5 w-5 text-blue-600" />
                <h3 className="section-title mb-0">Job Application Tracker</h3>
              </div>
              <Link to="/applications" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                View All Applications ({appStats?.totalApplications || 0})
                <ArrowRight className="h-3 w-3" />
              </Link>
            </div>

            {appStats && appStats.totalApplications > 0 ? (
              <div className="space-y-4">
                <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                  <div className="p-3 bg-blue-50/50 rounded-xl border border-blue-100 text-center">
                    <span className="text-[10px] font-bold text-surface-500 uppercase block">Active Pipeline</span>
                    <span className="text-xl font-extrabold text-blue-700">{appStats.activeCount || 0}</span>
                    <span className="text-[10px] text-surface-400 block mt-0.5">Applied & Screening</span>
                  </div>

                  <div className="p-3 bg-purple-50/50 rounded-xl border border-purple-100 text-center">
                    <span className="text-[10px] font-bold text-surface-500 uppercase block">Interviews</span>
                    <span className="text-xl font-extrabold text-purple-700">{appStats.interviewCount || 0}</span>
                    <span className="text-[10px] text-surface-400 block mt-0.5">{appStats.interviewRate}% Conversion</span>
                  </div>

                  <div className="p-3 bg-indigo-50/50 rounded-xl border border-indigo-100 text-center">
                    <span className="text-[10px] font-bold text-surface-500 uppercase block">Follow-ups Due</span>
                    <span className="text-xl font-extrabold text-indigo-700">
                      {upcomingTasks?.upcomingFollowUps?.length || 0}
                    </span>
                    <span className="text-[10px] text-surface-400 block mt-0.5">Scheduled Outreach</span>
                  </div>

                  <div className="p-3 bg-emerald-50/50 rounded-xl border border-emerald-100 text-center">
                    <span className="text-[10px] font-bold text-surface-500 uppercase block">Offers</span>
                    <span className="text-xl font-extrabold text-emerald-700">{appStats.offerCount || 0}</span>
                    <span className="text-[10px] text-surface-400 block mt-0.5">{appStats.offerRate}% Offer Rate</span>
                  </div>
                </div>

                {/* Priority alert banner for upcoming task */}
                {upcomingTasks?.upcomingInterviews && upcomingTasks.upcomingInterviews.length > 0 ? (
                  <div className="p-3 rounded-xl bg-gradient-to-r from-purple-50 to-indigo-50 border border-purple-200 flex items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-2">
                      <Mic className="h-4 w-4 text-purple-600 flex-shrink-0" />
                      <div>
                        <span className="font-bold text-surface-900">Upcoming Interview: </span>
                        <span className="text-surface-700">{upcomingTasks.upcomingInterviews[0].jobTitle} at {upcomingTasks.upcomingInterviews[0].company}</span>
                      </div>
                    </div>
                    <Link
                      to={`/mock-interview?targetRole=${encodeURIComponent(upcomingTasks.upcomingInterviews[0].jobTitle)}`}
                      className="btn-primary btn btn-xs flex-shrink-0"
                    >
                      Practice for This
                    </Link>
                  </div>
                ) : upcomingTasks?.upcomingFollowUps && upcomingTasks.upcomingFollowUps.length > 0 ? (
                  <div className="p-3 rounded-xl bg-indigo-50/60 border border-indigo-200 flex items-center justify-between gap-3 text-xs">
                    <div className="flex items-center gap-2">
                      <PhoneCall className="h-4 w-4 text-indigo-600 flex-shrink-0" />
                      <div>
                        <span className="font-bold text-surface-900">Follow-Up Due: </span>
                        <span className="text-surface-700">{upcomingTasks.upcomingFollowUps[0].jobTitle} ({upcomingTasks.upcomingFollowUps[0].company})</span>
                      </div>
                    </div>
                    <Link to="/applications" className="btn-secondary btn btn-xs flex-shrink-0">
                      View Details
                    </Link>
                  </div>
                ) : null}
              </div>
            ) : (
              <EmptyState
                icon={<ClipboardList />}
                title="Start tracking your job applications"
                description="Track applications across every stage from Saved to Offer, with automated timeline logging and AI strategy advice."
                action={
                  <div className="flex gap-2">
                    <Link to="/jobs" className="btn-primary btn btn-sm inline-flex items-center gap-1.5">
                      <Briefcase className="h-3.5 w-3.5" />
                      <span>Browse Jobs</span>
                    </Link>
                    <Link to="/applications" className="btn-secondary btn btn-sm inline-flex items-center gap-1.5">
                      <ClipboardList className="h-3.5 w-3.5" />
                      <span>Open Tracker</span>
                    </Link>
                  </div>
                }
              />
            )}
          </div>

          {/* Recommended Jobs */}
          {recommendedJobs.length > 0 && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5 text-primary-600" />
                  <h3 className="section-title mb-0">Recommended Jobs For You</h3>
                </div>
                <Link to="/jobs" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1 hover:underline">
                  View all jobs
                  <ArrowRight className="h-3 w-3" />
                </Link>
              </div>

              <div className="grid sm:grid-cols-3 gap-4">
                {recommendedJobs.map(({ job, match }) => (
                  <div key={job.id} className="card p-4 flex flex-col justify-between space-y-3 hover:shadow-md transition border border-surface-200">
                    <div className="space-y-1">
                      <div className="flex items-center justify-between gap-1">
                        <span className="text-[11px] font-semibold px-2 py-0.5 rounded bg-surface-100 text-surface-700 truncate">
                          {job.jobType ? job.jobType.replace('_', ' ') : 'Full Time'}
                        </span>
                        {match && (
                          <span className="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200">
                            {match.overallScore}% Match
                          </span>
                        )}
                      </div>
                      <h4 className="font-bold text-surface-900 text-sm line-clamp-1 mt-1">
                        {job.title}
                      </h4>
                      <div className="flex items-center gap-1.5 text-xs text-surface-500">
                        <Building2 className="h-3.5 w-3.5 text-surface-400 flex-shrink-0" />
                        <span className="truncate">{job.company}</span>
                      </div>
                      <div className="flex items-center gap-1.5 text-xs text-surface-400">
                        <MapPin className="h-3.5 w-3.5 text-surface-400 flex-shrink-0" />
                        <span className="truncate">{job.location}</span>
                      </div>
                    </div>

                    <Link
                      to="/jobs"
                      className="btn-secondary btn btn-xs w-full flex items-center justify-center gap-1"
                    >
                      <span>View Job</span>
                      <ArrowRight className="h-3 w-3" />
                    </Link>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* AI Career Assistant Feature Spotlight */}
          <div className="card p-6 bg-gradient-to-r from-indigo-900/20 via-purple-900/15 to-transparent border border-indigo-500/20 flex flex-col md:flex-row items-start md:items-center justify-between gap-4 shadow-sm">
            <div className="flex items-start gap-4">
              <div className="w-12 h-12 rounded-xl bg-gradient-to-tr from-indigo-600 to-purple-600 flex items-center justify-center text-white shadow-md shadow-indigo-500/20 flex-shrink-0">
                <Bot className="w-6 h-6" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-bold text-surface-900 text-base">AI Career Assistant</h3>
                  <span className="badge badge-primary text-xs">Ready</span>
                </div>
                <p className="text-surface-500 text-xs sm:text-sm mt-1 max-w-xl">
                  {profile?.targetRole 
                    ? `Get customized strategy, instant answers, and guidance for your ${profile.targetRole} career path grounded in your verified data.`
                    : 'Chat with your AI Career Copilot for personalized advice grounded in your resume, skill gaps, and interview scores.'}
                </p>
              </div>
            </div>
            <Link
              to="/ai-assistant"
              className="btn btn-primary flex items-center gap-2 flex-shrink-0 shadow-sm"
            >
              <MessageSquare className="w-4 h-4" />
              <span>Ask Career Copilot</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>

          {/* Recommended next steps */}
          <div>
            <h3 className="section-title">Recommended Next Steps</h3>
            <div className="grid sm:grid-cols-2 gap-4">
              {NEXT_STEPS.map(({ id, title, description, icon: Icon, to, cta, color }) => (
                <div key={id} className="card p-5 flex items-start gap-4">
                  <div className={`w-10 h-10 rounded-lg ${color} flex items-center justify-center flex-shrink-0`}>
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <h4 className="font-semibold text-surface-800 text-sm mb-1">{title}</h4>
                    <p className="text-xs text-surface-400 mb-3 leading-relaxed">{description}</p>
                    <Link to={to} className="btn-secondary btn btn-sm">
                      {cta}
                      <ArrowRight className="h-3 w-3" />
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Profile completeness */}
          {profile && (
            <div className="card p-5">
              <div className="flex items-center gap-2 mb-4">
                <User className="h-4 w-4 text-surface-500" />
                <h3 className="font-semibold text-surface-800 text-sm">Profile Completeness</h3>
              </div>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {[
                  { label: 'College', done: !!profile.college },
                  { label: 'Degree', done: !!profile.degree },
                  { label: 'Target Role', done: !!profile.targetRole },
                  { label: 'Skills', done: !!profile.skillsSummary },
                  { label: 'Experience Level', done: !!profile.experienceLevel },
                  { label: 'Graduation Year', done: !!profile.graduationYear },
                ].map(({ label, done }) => (
                  <div key={label} className="flex items-center gap-2">
                    <div className={`w-4 h-4 rounded-full flex items-center justify-center ${done ? 'bg-success-500' : 'bg-surface-200'}`}>
                      {done && (
                        <svg className="h-2.5 w-2.5 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                        </svg>
                      )}
                    </div>
                    <span className={`text-xs ${done ? 'text-surface-700' : 'text-surface-400'}`}>{label}</span>
                  </div>
                ))}
              </div>
              {!profile.onboardingComplete && (
                <Link to="/onboarding" className="btn-secondary btn btn-sm mt-4">
                  Complete Profile
                  <ArrowRight className="h-3 w-3" />
                </Link>
              )}
            </div>
          )}
        </div>
      )}
    </AppLayout>
  )
}
