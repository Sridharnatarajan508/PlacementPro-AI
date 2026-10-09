import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { authService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import { Eye, EyeOff, Bot, Loader2, CheckCircle } from 'lucide-react'
import toast from 'react-hot-toast'

const PASSWORD_RULES = [
  { label: 'At least 8 characters', test: (p) => p.length >= 8 },
  { label: 'Contains a letter', test: (p) => /[a-zA-Z]/.test(p) },
  { label: 'Contains a number', test: (p) => /\d/.test(p) },
]

export default function RegisterPage() {
  const [form, setForm] = useState({ name: '', email: '', password: '', confirmPassword: '' })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [errors, setErrors] = useState({})

  const { login } = useAuth()
  const navigate = useNavigate()

  const validate = () => {
    const errs = {}
    if (!form.name.trim()) errs.name = 'Full name is required'
    else if (form.name.trim().length < 2) errs.name = 'Name must be at least 2 characters'

    if (!form.email) errs.email = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) errs.email = 'Invalid email address'

    if (!form.password) errs.password = 'Password is required'
    else if (form.password.length < 8) errs.password = 'Password must be at least 8 characters'

    if (!form.confirmPassword) errs.confirmPassword = 'Please confirm your password'
    else if (form.password !== form.confirmPassword) errs.confirmPassword = 'Passwords do not match'

    return errs
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setLoading(true)

    try {
      const { data } = await authService.register({
        name: form.name.trim(),
        email: form.email,
        password: form.password,
      })
      login(data)
      toast.success(`Welcome, ${data.name}! Let's set up your profile.`)
      navigate('/onboarding')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left branding panel */}
      <div className="hidden lg:flex lg:w-1/2 bg-primary-600 flex-col justify-between p-12">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-white/20 flex items-center justify-center">
            <Bot className="h-5 w-5 text-white" />
          </div>
          <span className="text-white font-bold">AI Career Copilot</span>
        </div>
        <div>
          <h2 className="text-3xl font-bold text-white mb-4 leading-tight">
            Start your AI-powered career journey today
          </h2>
          {['Resume analysis with ATS score', 'Personalized skill gap analysis', 'AI-generated career roadmaps', 'Mock interview practice'].map((item) => (
            <div key={item} className="flex items-center gap-2.5 mb-3">
              <CheckCircle className="h-4 w-4 text-primary-200 flex-shrink-0" />
              <span className="text-primary-100 text-sm">{item}</span>
            </div>
          ))}
        </div>
        <p className="text-primary-300 text-sm">
          AI scores are advisory. Career outcomes depend on many factors.
        </p>
      </div>

      {/* Right form panel */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-12 overflow-y-auto">
        <div className="w-full max-w-sm py-8">
          {/* Mobile logo */}
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="w-7 h-7 rounded-lg bg-primary-600 flex items-center justify-center">
              <Bot className="h-4 w-4 text-white" />
            </div>
            <span className="font-bold text-surface-900 text-sm">AI Career Copilot</span>
          </div>

          <h1 className="text-2xl font-bold text-surface-900 mb-1">Create your account</h1>
          <p className="text-sm text-surface-500 mb-8">
            Already have an account?{' '}
            <Link to="/login" className="text-primary-600 font-medium hover:underline">
              Sign in
            </Link>
          </p>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="form-group">
              <label htmlFor="name" className="label">Full name</label>
              <input
                id="name"
                type="text"
                autoComplete="name"
                className={`input ${errors.name ? 'input-error' : ''}`}
                placeholder="Ravi Kumar"
                value={form.name}
                onChange={(e) => setForm(f => ({ ...f, name: e.target.value }))}
              />
              {errors.name && <p className="error-text">{errors.name}</p>}
            </div>

            <div className="form-group">
              <label htmlFor="reg-email" className="label">Email address</label>
              <input
                id="reg-email"
                type="email"
                autoComplete="email"
                className={`input ${errors.email ? 'input-error' : ''}`}
                placeholder="you@example.com"
                value={form.email}
                onChange={(e) => setForm(f => ({ ...f, email: e.target.value }))}
              />
              {errors.email && <p className="error-text">{errors.email}</p>}
            </div>

            <div className="form-group">
              <label htmlFor="reg-password" className="label">Password</label>
              <div className="relative">
                <input
                  id="reg-password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  className={`input pr-10 ${errors.password ? 'input-error' : ''}`}
                  placeholder="Min. 8 characters"
                  value={form.password}
                  onChange={(e) => setForm(f => ({ ...f, password: e.target.value }))}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(s => !s)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-surface-400 hover:text-surface-600"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
              {errors.password && <p className="error-text">{errors.password}</p>}
              {/* Password strength hints */}
              {form.password && (
                <div className="mt-2 space-y-1">
                  {PASSWORD_RULES.map(({ label, test }) => (
                    <div key={label} className="flex items-center gap-1.5">
                      <CheckCircle className={`h-3 w-3 ${test(form.password) ? 'text-success-500' : 'text-surface-300'}`} />
                      <span className={`text-xs ${test(form.password) ? 'text-success-700' : 'text-surface-400'}`}>
                        {label}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="form-group">
              <label htmlFor="confirm-password" className="label">Confirm password</label>
              <input
                id="confirm-password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="new-password"
                className={`input ${errors.confirmPassword ? 'input-error' : ''}`}
                placeholder="Re-enter password"
                value={form.confirmPassword}
                onChange={(e) => setForm(f => ({ ...f, confirmPassword: e.target.value }))}
              />
              {errors.confirmPassword && <p className="error-text">{errors.confirmPassword}</p>}
            </div>

            <button
              id="register-btn"
              type="submit"
              disabled={loading}
              className="btn-primary btn w-full mt-2"
            >
              {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
              {loading ? 'Creating account...' : 'Create Account'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
