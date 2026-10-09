import React, { useState, useEffect, useRef } from 'react'
import { useNavigate, useLocation, Link } from 'react-router-dom'
import {
  Bot, Send, Plus, Trash2, MessageSquare, Sparkles, ArrowRight,
  Briefcase, Zap, BookOpen, CheckCircle, HelpCircle, RefreshCw,
  Clock, AlertCircle, ChevronRight, User, Compass, TrendingUp,
  FileText, Mic, CheckCircle2, AlertTriangle, ShieldCheck
} from 'lucide-react'
import toast from 'react-hot-toast'
import AppLayout from '../components/layout/AppLayout'
import { careerAssistantService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'

const QUICK_PROMPTS = [
  {
    title: 'What should I learn next?',
    subtitle: 'Prioritize skills based on gaps & roadmap',
    prompt: 'What should I learn next based on my current skills, gaps, and target role?',
    icon: Zap,
    color: 'text-amber-600 bg-amber-50 border-amber-200'
  },
  {
    title: 'Am I ready for interviews?',
    subtitle: 'Evaluate mock interview & readiness scores',
    prompt: 'Am I ready for real technical interviews? Review my mock interview scores and readiness level.',
    icon: Mic,
    color: 'text-rose-600 bg-rose-50 border-rose-200'
  },
  {
    title: 'How can I improve my resume?',
    subtitle: 'ATS score & missing keywords',
    prompt: 'How can I improve my resume ATS score and highlight my technical strengths?',
    icon: FileText,
    color: 'text-blue-600 bg-blue-50 border-blue-200'
  },
  {
    title: 'What is my biggest skill gap?',
    subtitle: 'Analyze weaknesses & priority gaps',
    prompt: 'What is my biggest skill gap and how can I bridge it with practical projects?',
    icon: AlertCircle,
    color: 'text-purple-600 bg-purple-50 border-purple-200'
  },
  {
    title: 'Create my study plan for this week',
    subtitle: '7-day structured roadmap goals',
    prompt: 'Create a 7-day study and practice plan tailored to my roadmap milestones and analytics.',
    icon: BookOpen,
    color: 'text-indigo-600 bg-indigo-50 border-indigo-200'
  },
  {
    title: 'How can I improve my job readiness?',
    subtitle: 'Overall career intelligence recommendations',
    prompt: 'How can I improve my overall career readiness score to reach the READY level?',
    icon: TrendingUp,
    color: 'text-green-600 bg-green-50 border-green-200'
  }
]

const CONTEXT_PILLS = [
  { label: 'Resume', icon: FileText },
  { label: 'Skills', icon: Zap },
  { label: 'Job Matches', icon: Briefcase },
  { label: 'Mock Interviews', icon: Mic },
  { label: 'Roadmap', icon: Compass },
  { label: 'Analytics', icon: TrendingUp }
]

export default function AIAssistantPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const messagesEndRef = useRef(null)
  const textareaRef = useRef(null)

  const [conversations, setConversations] = useState([])
  const [activeConversationId, setActiveConversationId] = useState(null)
  const [messages, setMessages] = useState([])
  const [inputMessage, setInputMessage] = useState('')
  const [loadingConversations, setLoadingConversations] = useState(true)
  const [loadingMessages, setLoadingMessages] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState(null)
  const [rateLimited, setRateLimited] = useState(false)
  const [lastSentMessage, setLastSentMessage] = useState('')

  // Check URL query parameters for pre-filled / auto-sent prompts
  const initialPromptProcessed = useRef(false)

  useEffect(() => {
    loadConversations()
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, sending])

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  const loadConversations = async () => {
    setLoadingConversations(true)
    try {
      const res = await careerAssistantService.getConversations()
      const list = res.data || []
      setConversations(list)

      const queryParams = new URLSearchParams(location.search)
      const promptQuery = queryParams.get('prompt')

      if (promptQuery && !initialPromptProcessed.current) {
        initialPromptProcessed.current = true
        // Create new conversation and immediately send the prompt
        handleSendPromptDirect(promptQuery)
      } else if (list.length > 0) {
        selectConversation(list[0].id)
      } else {
        setActiveConversationId(null)
        setMessages([])
      }
    } catch (err) {
      console.error('Failed to load conversations:', err)
      toast.error('Failed to load conversations')
    } finally {
      setLoadingConversations(false)
    }
  }

  const selectConversation = async (convId) => {
    if (activeConversationId === convId && messages.length > 0) return
    setActiveConversationId(convId)
    setLoadingMessages(true)
    setError(null)
    setRateLimited(false)
    try {
      const res = await careerAssistantService.getConversation(convId)
      const data = res.data || {}
      setMessages(data.messages || [])
    } catch (err) {
      console.error('Failed to load messages:', err)
      setError('Could not load conversation messages.')
      toast.error('Failed to load conversation messages')
    } finally {
      setLoadingMessages(false)
      setTimeout(() => textareaRef.current?.focus(), 100)
    }
  }

  const handleNewConversation = () => {
    setActiveConversationId(null)
    setMessages([])
    setError(null)
    setRateLimited(false)
    setInputMessage('')
    setTimeout(() => textareaRef.current?.focus(), 100)
  }

  const handleDeleteConversation = async (e, convId) => {
    e.stopPropagation()
    if (!window.confirm('Are you sure you want to delete this conversation?')) return

    try {
      await careerAssistantService.deleteConversation(convId)
      toast.success('Conversation deleted')
      const updated = conversations.filter(c => c.id !== convId)
      setConversations(updated)

      if (activeConversationId === convId) {
        if (updated.length > 0) {
          selectConversation(updated[0].id)
        } else {
          handleNewConversation()
        }
      }
    } catch (err) {
      console.error('Failed to delete conversation:', err)
      toast.error('Could not delete conversation')
    }
  }

  const handleSendMessage = async (e) => {
    if (e) e.preventDefault()
    if (!inputMessage.trim() || sending) return

    const text = inputMessage.trim()
    setInputMessage('')
    await executeSend(text)
  }

  const handleSendPromptDirect = async (promptText) => {
    if (!promptText || sending) return
    await executeSend(promptText)
  }

  const executeSend = async (text) => {
    setLastSentMessage(text)
    setError(null)
    setRateLimited(false)

    // Optimistic user message update
    const optimisticUserMsg = {
      id: Date.now(),
      role: 'USER',
      content: text,
      createdAt: new Date().toISOString()
    }
    setMessages(prev => [...prev, optimisticUserMsg])
    setSending(true)

    try {
      let res
      if (activeConversationId) {
        res = await careerAssistantService.sendMessage(activeConversationId, text)
      } else {
        // Create conversation and send
        const createRes = await careerAssistantService.createConversation(text.slice(0, 40))
        const newConv = createRes.data
        setActiveConversationId(newConv.id)
        setConversations(prev => [newConv, ...prev])
        res = await careerAssistantService.sendMessage(newConv.id, text)
      }

      const replyData = res.data || {}
      const assistantMsg = {
        id: Date.now() + 1,
        role: 'ASSISTANT',
        content: replyData.message || replyData.answer || 'I evaluated your career profile. Let me know how else I can assist!',
        suggestedActions: replyData.suggestedActions || [],
        contextUsed: replyData.contextUsed || [],
        createdAt: replyData.createdAt || new Date().toISOString()
      }

      setMessages(prev => [...prev, assistantMsg])

      // Refresh conversations list in background to reflect updated title / timestamp
      careerAssistantService.getConversations().then(r => setConversations(r.data || [])).catch(() => {})
    } catch (err) {
      console.error('Career Assistant error:', err)
      const is429 = err?.response?.status === 429 || (err?.message && err.message.includes('429'))
      if (is429) {
        setRateLimited(true)
        setError('AI usage limit reached. Please wait a moment and click Retry.')
      } else {
        setError(getErrorMessage(err) || 'Failed to get a response from your AI Career Assistant. Please try again.')
      }
    } finally {
      setSending(false)
      setTimeout(() => textareaRef.current?.focus(), 100)
    }
  }

  const handleRetry = () => {
    if (lastSentMessage) {
      // Remove optimistic message if needed and resend
      executeSend(lastSentMessage)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage(e)
    }
  }

  return (
    <AppLayout>
      <div className="flex h-[calc(100vh-90px)] bg-surface-50 rounded-2xl border border-surface-200 overflow-hidden shadow-xs">
        
        {/* ============================================================ */}
        {/* LEFT SIDEBAR: Conversations List                             */}
        {/* ============================================================ */}
        <aside className="w-80 border-r border-surface-200 bg-white flex flex-col flex-shrink-0 hidden md:flex">
          {/* New Chat Button */}
          <div className="p-4 border-b border-surface-100">
            <button
              onClick={handleNewConversation}
              className="w-full btn-primary btn btn-sm flex items-center justify-center gap-2 py-2.5 shadow-xs"
            >
              <Plus className="h-4 w-4" />
              <span>New Conversation</span>
            </button>
          </div>

          {/* Conversations History */}
          <div className="flex-1 overflow-y-auto p-3 space-y-1">
            <div className="px-2 py-1 text-[11px] font-bold text-surface-400 uppercase tracking-wider">
              Recent Conversations
            </div>

            {loadingConversations ? (
              <div className="p-4 text-center text-xs text-surface-400 space-y-2">
                <RefreshCw className="h-4 w-4 animate-spin mx-auto text-primary-500" />
                <p>Loading history...</p>
              </div>
            ) : conversations.length === 0 ? (
              <div className="p-6 text-center text-xs text-surface-400">
                <MessageSquare className="h-6 w-6 mx-auto mb-2 text-surface-300 opacity-60" />
                <p>No conversations yet.</p>
                <p className="text-[11px] text-surface-400 mt-1">Start a new chat to ask career questions!</p>
              </div>
            ) : (
              conversations.map((c) => {
                const isActive = activeConversationId === c.id
                return (
                  <div
                    key={c.id}
                    onClick={() => selectConversation(c.id)}
                    className={`group relative flex items-center justify-between p-2.5 rounded-xl cursor-pointer text-xs transition-all ${
                      isActive
                        ? 'bg-primary-50 text-primary-900 font-semibold shadow-xs border border-primary-100'
                        : 'text-surface-700 hover:bg-surface-100 hover:text-surface-900'
                    }`}
                  >
                    <div className="flex items-center gap-2.5 min-w-0 pr-6">
                      <MessageSquare className={`h-4 w-4 flex-shrink-0 ${isActive ? 'text-primary-600' : 'text-surface-400'}`} />
                      <span className="truncate">{c.title || 'Conversation'}</span>
                    </div>

                    <button
                      onClick={(e) => handleDeleteConversation(e, c.id)}
                      className={`p-1 rounded-lg text-surface-400 hover:text-rose-600 hover:bg-rose-50 transition-colors ${
                        isActive ? 'opacity-100' : 'opacity-0 group-hover:opacity-100'
                      }`}
                      title="Delete conversation"
                    >
                      <Trash2 className="h-3.5 w-3.5" />
                    </button>
                  </div>
                )
              })
            )}
          </div>

          {/* Context status footer */}
          <div className="p-3 border-t border-surface-100 bg-surface-50/50">
            <div className="flex items-center gap-2 text-[11px] text-surface-500 font-medium">
              <ShieldCheck className="h-4 w-4 text-emerald-600" />
              <span>Full Candidate Profile Linked</span>
            </div>
          </div>
        </aside>

        {/* ============================================================ */}
        {/* MAIN CHAT AREA                                               */}
        {/* ============================================================ */}
        <main className="flex-1 flex flex-col bg-white overflow-hidden">
          
          {/* Header */}
          <header className="h-16 border-b border-surface-200 px-6 flex items-center justify-between bg-white z-10">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-primary-600 to-indigo-600 flex items-center justify-center text-white shadow-xs">
                <Bot className="h-5 w-5" />
              </div>
              <div>
                <h1 className="text-sm font-bold text-surface-900 leading-tight">
                  AI Career & Placement Copilot
                </h1>
                <p className="text-[11px] text-surface-500">
                  Personalized career strategy, placement prep & skill intelligence
                </p>
              </div>
            </div>

            {/* Career Context Indicator */}
            <div className="hidden lg:flex items-center gap-1.5 bg-surface-50 border border-surface-200 px-3 py-1 rounded-full text-[11px] text-surface-600">
              <span className="font-semibold text-surface-700 mr-1 flex items-center gap-1">
                <Sparkles className="h-3 w-3 text-primary-600" /> Context:
              </span>
              {CONTEXT_PILLS.map(pill => (
                <span key={pill.label} className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-white border border-surface-200 text-surface-700 font-medium">
                  <CheckCircle2 className="h-2.5 w-2.5 text-emerald-600" />
                  {pill.label}
                </span>
              ))}
            </div>
          </header>

          {/* Messages Scroll Area */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-surface-50/40">
            {messages.length === 0 && !loadingMessages ? (
              /* Welcome / Empty State View */
              <div className="max-w-2xl mx-auto py-8 text-center space-y-6 animate-fade-in">
                <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-primary-600 to-indigo-600 text-white flex items-center justify-center mx-auto shadow-md">
                  <Bot className="h-8 w-8" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-surface-900">
                    How can I help your career progression today?
                  </h2>
                  <p className="text-xs text-surface-500 max-w-md mx-auto mt-1.5 leading-relaxed">
                    I have full access to your Resume, Skill Gaps, Target Job Matches, Career Roadmap, Mock Interview scores, and Career Analytics.
                  </p>
                </div>

                {/* Quick Prompts Grid */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-left pt-2">
                  {QUICK_PROMPTS.map((item, idx) => {
                    const Icon = item.icon
                    return (
                      <button
                        key={idx}
                        onClick={() => handleSendPromptDirect(item.prompt)}
                        className="p-4 rounded-xl border border-surface-200 bg-white hover:border-primary-400 hover:shadow-xs transition-all flex items-start gap-3 text-left group"
                      >
                        <div className={`p-2 rounded-lg ${item.color} flex-shrink-0 group-hover:scale-105 transition-transform`}>
                          <Icon className="h-4 w-4" />
                        </div>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-surface-900 group-hover:text-primary-700 transition-colors">
                            {item.title}
                          </p>
                          <p className="text-[11px] text-surface-500 mt-0.5 truncate">
                            {item.subtitle}
                          </p>
                        </div>
                      </button>
                    )
                  })}
                </div>
              </div>
            ) : (
              /* Messages List */
              <div className="max-w-3xl mx-auto space-y-5">
                {messages.map((msg, idx) => {
                  const isUser = msg.role === 'USER'
                  return (
                    <div
                      key={msg.id || idx}
                      className={`flex gap-3.5 ${isUser ? 'justify-end' : 'justify-start'} animate-slide-up`}
                    >
                      {!isUser && (
                        <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-primary-600 to-indigo-600 text-white flex items-center justify-center flex-shrink-0 shadow-xs mt-1">
                          <Bot className="h-4 w-4" />
                        </div>
                      )}

                      <div
                        className={`max-w-[85%] rounded-2xl p-4 text-xs leading-relaxed shadow-xs ${
                          isUser
                            ? 'bg-primary-600 text-white rounded-tr-none'
                            : 'bg-white border border-surface-200 text-surface-800 rounded-tl-none'
                        }`}
                      >
                        {/* Text Message Content */}
                        <div className="whitespace-pre-wrap font-sans">
                          {msg.content || msg.message}
                        </div>

                        {/* Smart Suggested Actions */}
                        {!isUser && msg.suggestedActions && msg.suggestedActions.length > 0 && (
                          <div className="mt-4 pt-3 border-t border-surface-100">
                            <p className="text-[11px] font-bold text-surface-500 uppercase tracking-wider mb-2 flex items-center gap-1">
                              <Sparkles className="h-3 w-3 text-primary-600" />
                              Recommended Actions:
                            </p>
                            <div className="flex flex-wrap gap-2">
                              {msg.suggestedActions.map((act, aIdx) => {
                                const label = typeof act === 'string' ? act : act.label
                                const route = typeof act === 'object' && act.route ? act.route : '/dashboard'
                                return (
                                  <button
                                    key={aIdx}
                                    onClick={() => navigate(route)}
                                    className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-primary-50 hover:bg-primary-100 text-primary-700 font-semibold text-[11px] border border-primary-200 transition-colors"
                                  >
                                    <span>{label}</span>
                                    <ArrowRight className="h-3 w-3" />
                                  </button>
                                )
                              })}
                            </div>
                          </div>
                        )}

                        {/* Timestamp & Context Used Tags */}
                        <div className="flex items-center justify-between gap-3 mt-2 pt-1 text-[10px] opacity-70">
                          <span>
                            {msg.createdAt
                              ? new Date(msg.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
                              : ''}
                          </span>
                          {!isUser && msg.contextUsed && msg.contextUsed.length > 0 && (
                            <span className="italic">
                              Sources: {msg.contextUsed.join(', ')}
                            </span>
                          )}
                        </div>
                      </div>

                      {isUser && (
                        <div className="w-8 h-8 rounded-xl bg-surface-200 text-surface-700 flex items-center justify-center flex-shrink-0 mt-1 font-bold text-xs">
                          <User className="h-4 w-4" />
                        </div>
                      )}
                    </div>
                  )
                })}

                {/* Sending Spinner */}
                {sending && (
                  <div className="flex gap-3.5 justify-start animate-fade-in">
                    <div className="w-8 h-8 rounded-xl bg-gradient-to-tr from-primary-600 to-indigo-600 text-white flex items-center justify-center flex-shrink-0 shadow-xs">
                      <Bot className="h-4 w-4" />
                    </div>
                    <div className="bg-white border border-surface-200 rounded-2xl rounded-tl-none p-4 text-xs text-surface-600 flex items-center gap-2.5 shadow-xs">
                      <RefreshCw className="h-3.5 w-3.5 animate-spin text-primary-600" />
                      <span>Synthesizing your career context and evaluating placement guidance...</span>
                    </div>
                  </div>
                )}

                {/* Rate Limit / Error Alert */}
                {error && (
                  <div className={`p-4 rounded-xl border text-xs flex items-start justify-between gap-3 ${
                    rateLimited ? 'bg-amber-50 border-amber-200 text-amber-900' : 'bg-rose-50 border-rose-200 text-rose-900'
                  }`}>
                    <div className="flex items-center gap-2">
                      <AlertTriangle className="h-4 w-4 flex-shrink-0 text-amber-600" />
                      <span>{error}</span>
                    </div>
                    {rateLimited && (
                      <button
                        onClick={handleRetry}
                        className="btn-secondary btn btn-xs flex items-center gap-1 flex-shrink-0"
                      >
                        <RefreshCw className="h-3 w-3" />
                        <span>Retry</span>
                      </button>
                    )}
                  </div>
                )}

                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Input Bar */}
          <footer className="p-4 border-t border-surface-200 bg-white">
            <div className="max-w-3xl mx-auto">
              <form onSubmit={handleSendMessage} className="relative flex items-end gap-2 bg-surface-50 border border-surface-300 rounded-2xl p-2.5 focus-within:border-primary-500 focus-within:ring-2 focus-within:ring-primary-100 transition-all">
                <textarea
                  ref={textareaRef}
                  value={inputMessage}
                  onChange={(e) => setInputMessage(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Ask your career assistant anything (e.g. interview readiness, skill gaps, roadmap)..."
                  disabled={sending}
                  rows={2}
                  className="flex-1 bg-transparent border-0 resize-none text-xs text-surface-900 focus:outline-none placeholder:text-surface-400 p-1"
                />

                <div className="flex items-center gap-2 flex-shrink-0 pb-0.5">
                  <span className="text-[10px] text-surface-400 font-mono hidden sm:inline">
                    {inputMessage.length}/1000
                  </span>

                  <button
                    type="submit"
                    disabled={!inputMessage.trim() || sending}
                    className="p-2.5 rounded-xl bg-primary-600 hover:bg-primary-700 text-white disabled:opacity-40 disabled:hover:bg-primary-600 transition-colors shadow-xs"
                    title="Send Message (Enter)"
                  >
                    {sending ? (
                      <RefreshCw className="h-4 w-4 animate-spin" />
                    ) : (
                      <Send className="h-4 w-4" />
                    )}
                  </button>
                </div>
              </form>

              <p className="text-[10px] text-surface-400 text-center mt-2">
                Press <kbd className="px-1 py-0.5 rounded bg-surface-100 border border-surface-200 font-mono">Enter</kbd> to send, <kbd className="px-1 py-0.5 rounded bg-surface-100 border border-surface-200 font-mono">Shift + Enter</kbd> for new line.
              </p>
            </div>
          </footer>
        </main>
      </div>
    </AppLayout>
  )
}
