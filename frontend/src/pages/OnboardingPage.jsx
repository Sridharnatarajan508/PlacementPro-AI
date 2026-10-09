import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { profileService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import { Loader2, CheckCircle, GraduationCap, Target, Code2, User } from 'lucide-react'
import toast from 'react-hot-toast'

const STEPS = [
  { id: 'personal', label: 'Personal', icon: User },
  { id: 'education', label: 'Education', icon: GraduationCap },
  { id: 'career', label: 'Career', icon: Target },
  { id: 'skills', label: 'Skills', icon: Code2 },
]

const TARGET_ROLES = [
  'Java Developer', 'Full Stack Developer', 'Software Engineer',
  'Backend Developer', 'Frontend Developer', 'Data Analyst',
  'Data Engineer', 'DevOps Engineer', 'Python Developer', 'Other',
]

const EXP_LEVELS = ['Fresher', 'Intern', '1-2 years', '2-5 years']
const JOB_TYPES = ['Full-time', 'Internship', 'Part-time', 'Contract']
const CURRENT_YEAR = new Date().getFullYear()

export default function OnboardingPage() {
  const [step, setStep] = useState(0)
  const [loading, setLoading] = useState(false)
  const [form, setForm] = useState({
    college: '', degree: '', branch: '',
    graduationYear: '', cgpa: '',
    targetRole: '', preferredJobType: '', experienceLevel: '',
    skillsSummary: '',
  })

  const { user, updateUser } = useAuth()
  const navigate = useNavigate()

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }))

  const handleFinish = async () => {
    setLoading(true)
    try {
      const payload = {
        ...form,
        graduationYear: form.graduationYear ? parseInt(form.graduationYear) : null,
        cgpa: form.cgpa ? parseFloat(form.cgpa) : null,
        onboardingComplete: true,
      }
      await profileService.updateProfile(payload)
      updateUser({ onboardingComplete: true })
      toast.success('Profile complete! Welcome to your dashboard.')
      navigate('/dashboard')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const isLastStep = step === STEPS.length - 1

  return (
    <div className="min-h-screen bg-surface-50 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-lg">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-2xl font-bold text-surface-900">
            Welcome, {user?.name?.split(' ')[0]}! 👋
          </h1>
          <p className="text-surface-500 mt-1 text-sm">
            Let's set up your career profile. This takes about 2 minutes.
          </p>
        </div>

        {/* Step indicators */}
        <div className="flex items-center justify-center gap-2 mb-8">
          {STEPS.map((s, i) => (
            <div key={s.id} className="flex items-center">
              <button
                onClick={() => i < step && setStep(i)}
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold transition-all ${
                  i < step
                    ? 'bg-primary-600 text-white cursor-pointer'
                    : i === step
                    ? 'bg-primary-600 text-white ring-4 ring-primary-100'
                    : 'bg-surface-200 text-surface-400'
                }`}
              >
                {i < step ? <CheckCircle className="h-4 w-4" /> : i + 1}
              </button>
              {i < STEPS.length - 1 && (
                <div className={`w-12 h-0.5 mx-1 ${i < step ? 'bg-primary-400' : 'bg-surface-200'}`} />
              )}
            </div>
          ))}
        </div>

        {/* Card */}
        <div className="card p-6 sm:p-8">
          <div className="flex items-center gap-2 mb-6">
            {(() => { const Icon = STEPS[step].icon; return <Icon className="h-5 w-5 text-primary-600" /> })()}
            <h2 className="text-lg font-semibold text-surface-800">{STEPS[step].label} Information</h2>
          </div>

          {/* Step 0: Personal (pre-filled) */}
          {step === 0 && (
            <div className="space-y-4">
              <div className="p-4 bg-surface-50 rounded-lg">
                <p className="text-xs text-surface-500 mb-1">Name</p>
                <p className="font-medium text-surface-800">{user?.name}</p>
              </div>
              <div className="p-4 bg-surface-50 rounded-lg">
                <p className="text-xs text-surface-500 mb-1">Email</p>
                <p className="font-medium text-surface-800">{user?.email}</p>
              </div>
              <p className="text-xs text-surface-400">You can update your name and email from the Profile page later.</p>
            </div>
          )}

          {/* Step 1: Education */}
          {step === 1 && (
            <div className="space-y-4">
              <div className="form-group">
                <label className="label">College / University</label>
                <input className="input" placeholder="e.g. Anna University" value={form.college} onChange={e => set('college', e.target.value)} />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="form-group">
                  <label className="label">Degree</label>
                  <input className="input" placeholder="e.g. B.E., B.Tech" value={form.degree} onChange={e => set('degree', e.target.value)} />
                </div>
                <div className="form-group">
                  <label className="label">Branch</label>
                  <input className="input" placeholder="e.g. CSE, ECE" value={form.branch} onChange={e => set('branch', e.target.value)} />
                </div>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="form-group">
                  <label className="label">Graduation Year</label>
                  <select className="input" value={form.graduationYear} onChange={e => set('graduationYear', e.target.value)}>
                    <option value="">Select year</option>
                    {Array.from({ length: 8 }, (_, i) => CURRENT_YEAR - 2 + i).map(y => (
                      <option key={y} value={y}>{y}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label className="label">CGPA <span className="text-surface-400 font-normal">(optional)</span></label>
                  <input className="input" type="number" min="0" max="10" step="0.01" placeholder="e.g. 8.5" value={form.cgpa} onChange={e => set('cgpa', e.target.value)} />
                </div>
              </div>
            </div>
          )}

          {/* Step 2: Career */}
          {step === 2 && (
            <div className="space-y-4">
              <div className="form-group">
                <label className="label">Target Role</label>
                <select className="input" value={form.targetRole} onChange={e => set('targetRole', e.target.value)}>
                  <option value="">Select a role</option>
                  {TARGET_ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
                {form.targetRole === 'Other' && (
                  <input className="input mt-2" placeholder="Enter your target role" onChange={e => set('targetRole', e.target.value)} />
                )}
              </div>
              <div className="form-group">
                <label className="label">Experience Level</label>
                <div className="grid grid-cols-2 gap-2">
                  {EXP_LEVELS.map(level => (
                    <button
                      key={level}
                      type="button"
                      onClick={() => set('experienceLevel', level)}
                      className={`py-2 px-3 text-sm rounded-lg border transition-all ${
                        form.experienceLevel === level
                          ? 'border-primary-500 bg-primary-50 text-primary-700 font-medium'
                          : 'border-surface-200 text-surface-600 hover:border-surface-300'
                      }`}
                    >
                      {level}
                    </button>
                  ))}
                </div>
              </div>
              <div className="form-group">
                <label className="label">Preferred Job Type</label>
                <div className="grid grid-cols-2 gap-2">
                  {JOB_TYPES.map(type => (
                    <button
                      key={type}
                      type="button"
                      onClick={() => set('preferredJobType', type)}
                      className={`py-2 px-3 text-sm rounded-lg border transition-all ${
                        form.preferredJobType === type
                          ? 'border-primary-500 bg-primary-50 text-primary-700 font-medium'
                          : 'border-surface-200 text-surface-600 hover:border-surface-300'
                      }`}
                    >
                      {type}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 3: Skills */}
          {step === 3 && (
            <div className="space-y-4">
              <div className="form-group">
                <label className="label">Current Skills</label>
                <textarea
                  className="input resize-none"
                  rows={5}
                  placeholder="List your skills separated by commas.&#10;e.g. Java, Python, SQL, React, Git, Spring Boot, HTML, CSS"
                  value={form.skillsSummary}
                  onChange={e => set('skillsSummary', e.target.value)}
                />
                <p className="text-xs text-surface-400 mt-1">
                  You can update and expand your skills at any time from the Profile page.
                </p>
              </div>
              {form.skillsSummary && (
                <div className="flex flex-wrap gap-1.5">
                  {form.skillsSummary.split(',').map(s => s.trim()).filter(Boolean).map(skill => (
                    <span key={skill} className="badge-primary">{skill}</span>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* Navigation */}
          <div className="flex justify-between mt-8">
            <button
              type="button"
              onClick={() => setStep(s => s - 1)}
              disabled={step === 0}
              className="btn-secondary btn"
            >
              Back
            </button>
            {isLastStep ? (
              <button
                type="button"
                onClick={handleFinish}
                disabled={loading}
                className="btn-primary btn"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : null}
                {loading ? 'Saving...' : 'Complete Setup'}
              </button>
            ) : (
              <button
                type="button"
                onClick={() => setStep(s => s + 1)}
                className="btn-primary btn"
              >
                Continue
              </button>
            )}
          </div>
        </div>

        <p className="text-center text-xs text-surface-400 mt-4">
          You can skip any step and complete your profile later.
        </p>
      </div>
    </div>
  )
}
