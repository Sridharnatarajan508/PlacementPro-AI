import { Link } from 'react-router-dom'
import {
  Bot, FileText, Briefcase, Zap, Map, Mic, ClipboardList,
  MessageSquare, ArrowRight, CheckCircle, Star, Sparkles
} from 'lucide-react'

const FEATURES = [
  {
    icon: FileText,
    title: 'AI Resume Analyzer',
    description: 'Get an ATS score, detect skills, find weaknesses, and receive specific improvement suggestions.',
    color: 'bg-blue-50 text-blue-600',
  },
  {
    icon: Briefcase,
    title: 'Job Matcher',
    description: 'Paste any job description and get an AI-powered compatibility score with gap analysis.',
    color: 'bg-purple-50 text-purple-600',
  },
  {
    icon: Zap,
    title: 'Skill Gap Analyzer',
    description: 'Know exactly which skills to learn next, prioritized for your target role.',
    color: 'bg-amber-50 text-amber-600',
  },
  {
    icon: Map,
    title: 'Career Roadmap',
    description: 'Get a personalized, phase-by-phase learning roadmap based on your current profile.',
    color: 'bg-green-50 text-green-600',
  },
  {
    icon: Mic,
    title: 'AI Mock Interviews',
    description: 'Practice with AI-generated questions and receive detailed feedback on your answers.',
    color: 'bg-rose-50 text-rose-600',
  },
  {
    icon: MessageSquare,
    title: 'Career Assistant',
    description: 'Chat with an AI that knows your profile, skills, and goals to give personalized advice.',
    color: 'bg-indigo-50 text-indigo-600',
  },
]

const HOW_IT_WORKS = [
  { step: '01', title: 'Create Your Profile', desc: 'Share your education, skills, and target role.' },
  { step: '02', title: 'Upload Your Resume', desc: 'Get instant AI analysis and an ATS compatibility score.' },
  { step: '03', title: 'Get Your Roadmap', desc: 'Follow a personalized learning path to fill your skill gaps.' },
  { step: '04', title: 'Practice & Track', desc: 'Take mock interviews and track every job application.' },
]

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Navbar */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-sm border-b border-surface-200">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center">
              <Bot className="h-5 w-5 text-white" />
            </div>
            <span className="text-sm font-bold text-surface-900">AI Career Copilot</span>
          </div>
          <div className="flex items-center gap-3">
            <Link to="/login" className="btn-ghost btn text-sm">
              Sign In
            </Link>
            <Link to="/register" className="btn-primary btn text-sm">
              Get Started
            </Link>
          </div>
        </div>
      </nav>

      {/* Hero */}
      <section className="pt-32 pb-20 px-4 sm:px-6">
        <div className="max-w-4xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full bg-primary-50 border border-primary-100 mb-6">
            <Sparkles className="h-3.5 w-3.5 text-primary-600" />
            <span className="text-xs font-medium text-primary-700">Powered by Google Gemini AI</span>
          </div>

          <h1 className="text-4xl sm:text-5xl md:text-6xl font-extrabold text-surface-900 leading-tight mb-6">
            AI Career &amp;{' '}
            <span className="text-primary-600">Placement</span>{' '}
            Copilot
          </h1>

          <p className="text-lg sm:text-xl text-surface-500 max-w-2xl mx-auto mb-10 leading-relaxed">
            Your AI-powered personal assistant for resumes, jobs, skills, interviews, and placement preparation.
            Built for college students and fresh graduates.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <Link to="/register" className="btn-primary btn btn-lg w-full sm:w-auto">
              Start for Free
              <ArrowRight className="h-4 w-4" />
            </Link>
            <Link to="/login" className="btn-secondary btn btn-lg w-full sm:w-auto">
              Sign In
            </Link>
          </div>

          <div className="flex flex-wrap items-center justify-center gap-6 mt-10 text-sm text-surface-400">
            {['No credit card required', 'AI-powered analysis', 'Real-time feedback'].map((item) => (
              <div key={item} className="flex items-center gap-1.5">
                <CheckCircle className="h-3.5 w-3.5 text-success-500" />
                <span>{item}</span>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Stats bar */}
      <section className="py-10 bg-surface-50 border-y border-surface-200">
        <div className="max-w-4xl mx-auto px-4 grid grid-cols-2 md:grid-cols-4 gap-8 text-center">
          {[
            { value: 'AI-Powered', label: 'Resume Analysis' },
            { value: '10+', label: 'Platform Features' },
            { value: 'Real-time', label: 'Interview Feedback' },
            { value: 'Personalized', label: 'Career Roadmaps' },
          ].map(({ value, label }) => (
            <div key={label}>
              <p className="text-2xl font-bold text-surface-900">{value}</p>
              <p className="text-sm text-surface-400 mt-1">{label}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Features */}
      <section className="py-20 px-4 sm:px-6">
        <div className="max-w-6xl mx-auto">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-bold text-surface-900 mb-3">
              Everything you need to land your dream job
            </h2>
            <p className="text-surface-500 max-w-xl mx-auto">
              From resume review to mock interviews — all in one platform.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6">
            {FEATURES.map(({ icon: Icon, title, description, color }) => (
              <div key={title} className="card p-6 hover:shadow-card-hover transition-shadow">
                <div className={`w-10 h-10 rounded-lg ${color} flex items-center justify-center mb-4`}>
                  <Icon className="h-5 w-5" />
                </div>
                <h3 className="font-semibold text-surface-900 mb-2">{title}</h3>
                <p className="text-sm text-surface-500 leading-relaxed">{description}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* How it works */}
      <section className="py-20 px-4 sm:px-6 bg-surface-50 border-t border-surface-200">
        <div className="max-w-4xl mx-auto">
          <div className="text-center mb-14">
            <h2 className="text-3xl font-bold text-surface-900 mb-3">How it works</h2>
            <p className="text-surface-500">Get started in minutes and see results immediately.</p>
          </div>

          <div className="grid sm:grid-cols-2 md:grid-cols-4 gap-8">
            {HOW_IT_WORKS.map(({ step, title, desc }) => (
              <div key={step} className="text-center">
                <div className="w-12 h-12 rounded-xl bg-primary-600 text-white font-bold text-lg flex items-center justify-center mx-auto mb-4">
                  {step}
                </div>
                <h3 className="font-semibold text-surface-900 mb-1.5">{title}</h3>
                <p className="text-sm text-surface-400">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-4 sm:px-6">
        <div className="max-w-2xl mx-auto text-center">
          <h2 className="text-3xl font-bold text-surface-900 mb-4">
            Ready to accelerate your placement?
          </h2>
          <p className="text-surface-500 mb-8">
            Join thousands of students already using AI to prepare smarter.
          </p>
          <Link to="/register" className="btn-primary btn btn-lg">
            Create Your Free Account
            <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-surface-200 py-8 px-4 sm:px-6">
        <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-md bg-primary-600 flex items-center justify-center">
              <Bot className="h-3.5 w-3.5 text-white" />
            </div>
            <span className="text-sm font-semibold text-surface-700">AI Career Copilot</span>
          </div>
          <p className="text-xs text-surface-400">
            AI scores are advisory indicators. Career outcomes depend on many factors.
          </p>
        </div>
      </footer>
    </div>
  )
}
