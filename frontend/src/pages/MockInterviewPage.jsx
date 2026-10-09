import { useState, useEffect, useCallback } from 'react'
import { toast } from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { interviewService, profileService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'

import InterviewSetup from '../components/interview/InterviewSetup'
import InterviewProgress from '../components/interview/InterviewProgress'
import InterviewQuestion from '../components/interview/InterviewQuestion'
import AnswerFeedback from '../components/interview/AnswerFeedback'
import InterviewResult from '../components/interview/InterviewResult'
import InterviewHistory from '../components/interview/InterviewHistory'

import { Mic, Sparkles, History, Play } from 'lucide-react'

export default function MockInterviewPage() {
  const [profile, setProfile] = useState(null)
  const [view, setView] = useState('setup') // 'setup' | 'active' | 'result' | 'history'
  const [session, setSession] = useState(null)
  const [currentQuestion, setCurrentQuestion] = useState(null)
  const [latestEvaluation, setLatestEvaluation] = useState(null)
  const [pendingNextQuestion, setPendingNextQuestion] = useState(null)
  const [isInterviewComplete, setIsInterviewComplete] = useState(false)
  const [historyList, setHistoryList] = useState([])

  const [loadingInitial, setLoadingInitial] = useState(true)
  const [starting, setStarting] = useState(false)
  const [submittingAnswer, setSubmittingAnswer] = useState(false)

  // Load Profile & History on mount
  const loadInitialData = useCallback(async () => {
    setLoadingInitial(true)
    try {
      try {
        const { data: prof } = await profileService.getProfile()
        setProfile(prof)
      } catch (e) {}

      try {
        const { data: hist } = await interviewService.getInterviewHistory()
        setHistoryList(hist || [])
      } catch (e) {}
    } finally {
      setLoadingInitial(false)
    }
  }, [])

  useEffect(() => {
    loadInitialData()
  }, [loadInitialData])

  // 1. Start Interview
  const handleStartInterview = async (config) => {
    setStarting(true)
    try {
      const { data } = await interviewService.startInterview(config)
      setSession(data)
      setIsInterviewComplete(false)
      setLatestEvaluation(null)
      setPendingNextQuestion(null)

      if (data.questions && data.questions.length > 0) {
        const q1 = data.questions[0]
        setCurrentQuestion({
          id: q1.id,
          questionNumber: q1.questionNumber,
          questionText: q1.questionText,
          category: q1.category,
          difficulty: q1.difficulty,
          totalQuestions: data.totalQuestions,
        })
      }
      setView('active')
      toast.success('Mock interview started! Good luck!')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setStarting(false)
    }
  }

  // 2. Submit Answer
  const handleSubmitAnswer = async (questionId, answer) => {
    if (!session) return
    setSubmittingAnswer(true)
    try {
      const { data } = await interviewService.submitAnswer(session.id, questionId, { answer })
      setLatestEvaluation(data)

      if (data.isComplete) {
        setIsInterviewComplete(true)
        if (data.finalSessionResult) {
          setSession(data.finalSessionResult)
        }
        toast.success('Interview complete! Preparing final evaluation...')
      } else {
        setPendingNextQuestion(data.nextQuestion)
        toast.success('Answer evaluated!')
      }
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setSubmittingAnswer(false)
    }
  }

  // 3. Move to next question or result
  const handleNextStep = () => {
    if (isInterviewComplete) {
      setView('result')
      setLatestEvaluation(null)
    } else if (pendingNextQuestion) {
      setCurrentQuestion(pendingNextQuestion)
      setPendingNextQuestion(null)
      setLatestEvaluation(null)
    }
  }

  // 4. Abandon Interview
  const handleAbandon = async () => {
    if (!session) return
    if (!window.confirm('Are you sure you want to exit this mock interview session?')) return

    try {
      await interviewService.abandonInterview(session.id)
      toast('Interview ended', { icon: 'ℹ️' })
      refreshHistory()
      setView('history')
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  // 5. Select from history
  const handleSelectHistorySession = async (sId) => {
    try {
      const { data } = await interviewService.getInterview(sId)
      setSession(data)
      if (data.status === 'COMPLETED') {
        setView('result')
      } else if (data.status === 'IN_PROGRESS') {
        // Resume active session
        try {
          const { data: nextQ } = await interviewService.getNextQuestion(sId)
          setCurrentQuestion(nextQ)
          setLatestEvaluation(null)
          setView('active')
        } catch (e) {
          setView('result')
        }
      } else {
        setView('result')
      }
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  const refreshHistory = async () => {
    try {
      const { data } = await interviewService.getInterviewHistory()
      setHistoryList(data || [])
    } catch (e) {}
  }

  const handleOpenHistory = () => {
    refreshHistory()
    setView('history')
  }

  return (
    <AppLayout>
      <div className="space-y-6 pb-12 animate-slide-up">
        {/* Navigation Tabs if not in active interview */}
        {view !== 'active' && (
          <div className="flex items-center justify-between border-b border-surface-200 pb-3">
            <div className="flex items-center gap-2">
              <Mic className="h-6 w-6 text-primary-600" />
              <div>
                <h1 className="text-xl font-bold text-surface-900 leading-tight">
                  AI Mock Interview & Evaluation
                </h1>
                <p className="text-xs text-surface-500">
                  Simulate real-world technical and behavioral interviews with real-time AI scoring and guidance.
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setView('setup')}
                className={`btn btn-sm ${view === 'setup' ? 'btn-primary' : 'btn-secondary'} inline-flex items-center gap-1.5`}
              >
                <Play className="h-3.5 w-3.5" />
                <span>New Interview</span>
              </button>

              <button
                onClick={handleOpenHistory}
                className={`btn btn-sm ${view === 'history' ? 'btn-primary' : 'btn-secondary'} inline-flex items-center gap-1.5`}
              >
                <History className="h-3.5 w-3.5" />
                <span>History ({historyList.length})</span>
              </button>
            </div>
          </div>
        )}

        {/* View Switcher */}
        {loadingInitial ? (
          <LoadingSpinner text="Loading interview module..." />
        ) : view === 'setup' ? (
          <InterviewSetup
            initialRole={profile?.targetRole || ''}
            onStart={handleStartInterview}
            starting={starting}
          />
        ) : view === 'active' ? (
          <div className="space-y-6 max-w-3xl mx-auto">
            {/* Progress Header */}
            <InterviewProgress
              currentNumber={currentQuestion?.questionNumber || 1}
              totalQuestions={session?.totalQuestions || 10}
              targetRole={session?.targetRole || 'Software Engineer'}
              interviewType={session?.interviewType || 'MIXED'}
              difficulty={session?.difficulty || 'MEDIUM'}
              onAbandon={handleAbandon}
            />

            {/* Answer feedback OR Active Question */}
            {latestEvaluation ? (
              <AnswerFeedback
                evaluation={latestEvaluation}
                onNext={handleNextStep}
                isComplete={isInterviewComplete}
              />
            ) : (
              <InterviewQuestion
                question={currentQuestion}
                onSubmitAnswer={handleSubmitAnswer}
                submitting={submittingAnswer}
              />
            )}
          </div>
        ) : view === 'result' ? (
          <InterviewResult
            session={session}
            onRetake={() => setView('setup')}
            onViewHistory={handleOpenHistory}
          />
        ) : view === 'history' ? (
          <InterviewHistory
            history={historyList}
            onSelectSession={handleSelectHistorySession}
            onNewInterview={() => setView('setup')}
          />
        ) : null}
      </div>
    </AppLayout>
  )
}
