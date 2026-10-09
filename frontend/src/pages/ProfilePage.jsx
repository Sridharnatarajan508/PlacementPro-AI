import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'
import { profileService } from '../services/api'
import { getErrorMessage } from '../services/apiClient'
import AppLayout from '../components/layout/AppLayout'
import LoadingSpinner from '../components/ui/LoadingSpinner'
import { Loader2, Edit2, Check, X } from 'lucide-react'
import toast from 'react-hot-toast'

const TARGET_ROLES = [
  'Java Developer', 'Full Stack Developer', 'Software Engineer',
  'Backend Developer', 'Frontend Developer', 'Data Analyst',
  'Data Engineer', 'DevOps Engineer', 'Python Developer', 'Other',
]

export default function ProfilePage() {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState({})

  useEffect(() => {
    profileService.getProfile()
      .then(({ data }) => {
        setProfile(data)
        setForm({
          college: data.college || '',
          degree: data.degree || '',
          branch: data.branch || '',
          graduationYear: data.graduationYear || '',
          cgpa: data.cgpa || '',
          targetRole: data.targetRole || '',
          preferredJobType: data.preferredJobType || '',
          experienceLevel: data.experienceLevel || '',
          skillsSummary: data.skillsSummary || '',
        })
      })
      .catch(() => toast.error('Failed to load profile'))
      .finally(() => setLoading(false))
  }, [])

  const handleSave = async () => {
    setSaving(true)
    try {
      const { data } = await profileService.updateProfile({
        ...form,
        graduationYear: form.graduationYear ? parseInt(form.graduationYear) : null,
        cgpa: form.cgpa ? parseFloat(form.cgpa) : null,
      })
      setProfile(data)
      setEditing(false)
      toast.success('Profile updated successfully')
    } catch (err) {
      toast.error(getErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const set = (key, val) => setForm(f => ({ ...f, [key]: val }))

  const Field = ({ label, value, children }) => (
    <div>
      <p className="text-xs font-medium text-surface-400 uppercase tracking-wide mb-1">{label}</p>
      {editing ? children : (
        <p className="text-sm text-surface-800 font-medium">{value || <span className="text-surface-300 font-normal">Not set</span>}</p>
      )}
    </div>
  )

  return (
    <AppLayout>
      {loading ? (
        <LoadingSpinner text="Loading profile..." />
      ) : (
        <div className="max-w-2xl space-y-6 animate-slide-up">
          {/* Header */}
          <div className="flex items-start justify-between">
            <div className="page-header mb-0">
              <h2 className="page-title">My Profile</h2>
              <p className="page-subtitle">Manage your personal and career information.</p>
            </div>
            {!editing ? (
              <button onClick={() => setEditing(true)} className="btn-secondary btn btn-sm">
                <Edit2 className="h-3.5 w-3.5" /> Edit
              </button>
            ) : (
              <div className="flex gap-2">
                <button onClick={() => setEditing(false)} className="btn-secondary btn btn-sm">
                  <X className="h-3.5 w-3.5" /> Cancel
                </button>
                <button onClick={handleSave} disabled={saving} className="btn-primary btn btn-sm">
                  {saving ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : <Check className="h-3.5 w-3.5" />}
                  Save
                </button>
              </div>
            )}
          </div>

          {/* Account info */}
          <div className="card p-6">
            <h3 className="text-sm font-semibold text-surface-700 mb-4">Account</h3>
            <div className="space-y-4">
              <div>
                <p className="text-xs font-medium text-surface-400 uppercase tracking-wide mb-1">Full Name</p>
                <p className="text-sm font-medium text-surface-800">{user?.name}</p>
              </div>
              <div>
                <p className="text-xs font-medium text-surface-400 uppercase tracking-wide mb-1">Email</p>
                <p className="text-sm font-medium text-surface-800">{user?.email}</p>
              </div>
            </div>
          </div>

          {/* Education */}
          <div className="card p-6">
            <h3 className="text-sm font-semibold text-surface-700 mb-4">Education</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field label="College" value={profile?.college}>
                <input className="input" value={form.college} onChange={e => set('college', e.target.value)} placeholder="University name" />
              </Field>
              <Field label="Degree" value={profile?.degree}>
                <input className="input" value={form.degree} onChange={e => set('degree', e.target.value)} placeholder="e.g. B.Tech" />
              </Field>
              <Field label="Branch" value={profile?.branch}>
                <input className="input" value={form.branch} onChange={e => set('branch', e.target.value)} placeholder="e.g. CSE" />
              </Field>
              <Field label="Graduation Year" value={profile?.graduationYear}>
                <input className="input" type="number" value={form.graduationYear} onChange={e => set('graduationYear', e.target.value)} placeholder="e.g. 2025" />
              </Field>
              <Field label="CGPA" value={profile?.cgpa}>
                <input className="input" type="number" step="0.01" min="0" max="10" value={form.cgpa} onChange={e => set('cgpa', e.target.value)} placeholder="e.g. 8.5" />
              </Field>
            </div>
          </div>

          {/* Career */}
          <div className="card p-6">
            <h3 className="text-sm font-semibold text-surface-700 mb-4">Career Goals</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <Field label="Target Role" value={profile?.targetRole}>
                <select className="input" value={form.targetRole} onChange={e => set('targetRole', e.target.value)}>
                  <option value="">Select role</option>
                  {TARGET_ROLES.map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </Field>
              <Field label="Experience Level" value={profile?.experienceLevel}>
                <select className="input" value={form.experienceLevel} onChange={e => set('experienceLevel', e.target.value)}>
                  <option value="">Select level</option>
                  {['Fresher', 'Intern', '1-2 years', '2-5 years'].map(l => <option key={l} value={l}>{l}</option>)}
                </select>
              </Field>
              <Field label="Preferred Job Type" value={profile?.preferredJobType}>
                <select className="input" value={form.preferredJobType} onChange={e => set('preferredJobType', e.target.value)}>
                  <option value="">Select type</option>
                  {['Full-time', 'Internship', 'Part-time', 'Contract'].map(t => <option key={t} value={t}>{t}</option>)}
                </select>
              </Field>
            </div>
          </div>

          {/* Skills */}
          <div className="card p-6">
            <h3 className="text-sm font-semibold text-surface-700 mb-4">Skills</h3>
            {editing ? (
              <div className="form-group">
                <label className="label">Skills (comma-separated)</label>
                <textarea
                  className="input resize-none"
                  rows={3}
                  value={form.skillsSummary}
                  onChange={e => set('skillsSummary', e.target.value)}
                  placeholder="Java, Python, SQL, React, Git..."
                />
              </div>
            ) : (
              profile?.skillsSummary ? (
                <div className="flex flex-wrap gap-1.5">
                  {profile.skillsSummary.split(',').map(s => s.trim()).filter(Boolean).map(skill => (
                    <span key={skill} className="badge-primary">{skill}</span>
                  ))}
                </div>
              ) : (
                <p className="text-sm text-surface-300">No skills added yet. Click Edit to add your skills.</p>
              )
            )}
          </div>
        </div>
      )}
    </AppLayout>
  )
}
