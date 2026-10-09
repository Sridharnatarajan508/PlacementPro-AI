import AppLayout from '../components/layout/AppLayout'
import EmptyState from '../components/ui/EmptyState'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { LogOut, Lock, Trash2, AlertTriangle } from 'lucide-react'
import { useState } from 'react'
import toast from 'react-hot-toast'

export default function SettingsPage() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)

  const handleLogout = () => {
    logout()
    navigate('/login')
    toast.success('You have been signed out.')
  }

  return (
    <AppLayout>
      <div className="max-w-xl space-y-6 animate-slide-up">
        <div className="page-header">
          <h2 className="page-title">Settings</h2>
          <p className="page-subtitle">Manage your account preferences.</p>
        </div>

        {/* Account Info */}
        <div className="card p-6">
          <h3 className="text-sm font-semibold text-surface-700 mb-4">Account Information</h3>
          <div className="space-y-3">
            <div className="flex justify-between items-center py-2 border-b border-surface-100">
              <span className="text-sm text-surface-500">Name</span>
              <span className="text-sm font-medium text-surface-800">{user?.name}</span>
            </div>
            <div className="flex justify-between items-center py-2 border-b border-surface-100">
              <span className="text-sm text-surface-500">Email</span>
              <span className="text-sm font-medium text-surface-800">{user?.email}</span>
            </div>
            <div className="flex justify-between items-center py-2">
              <span className="text-sm text-surface-500">Role</span>
              <span className="badge-primary capitalize">{user?.role?.toLowerCase()}</span>
            </div>
          </div>
        </div>

        {/* Password Change — Phase 15+ */}
        <div className="card p-6 opacity-60">
          <div className="flex items-center gap-2 mb-3">
            <Lock className="h-4 w-4 text-surface-400" />
            <h3 className="text-sm font-semibold text-surface-700">Change Password</h3>
            <span className="badge-surface">Coming Soon</span>
          </div>
          <p className="text-xs text-surface-400">Password change will be available in a future update.</p>
        </div>

        {/* Logout */}
        <div className="card p-6">
          <h3 className="text-sm font-semibold text-surface-700 mb-3">Session</h3>
          <button
            id="logout-btn"
            onClick={handleLogout}
            className="btn-secondary btn text-danger-600 hover:bg-danger-50 hover:border-danger-200 hover:text-danger-700"
          >
            <LogOut className="h-4 w-4" />
            Sign Out
          </button>
        </div>

        {/* Danger Zone */}
        <div className="card p-6 border-danger-200">
          <div className="flex items-center gap-2 mb-3">
            <AlertTriangle className="h-4 w-4 text-danger-500" />
            <h3 className="text-sm font-semibold text-danger-700">Danger Zone</h3>
          </div>
          {!showDeleteConfirm ? (
            <button
              onClick={() => setShowDeleteConfirm(true)}
              className="btn-secondary btn btn-sm text-danger-600 hover:bg-danger-50"
            >
              <Trash2 className="h-3.5 w-3.5" />
              Delete Account
            </button>
          ) : (
            <div className="bg-danger-50 rounded-lg p-4 space-y-3">
              <p className="text-sm font-medium text-danger-800">
                Are you sure? This action cannot be undone. All your data will be permanently deleted.
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => {
                    toast.error('Account deletion will be available in a future update.')
                    setShowDeleteConfirm(false)
                  }}
                  className="btn-danger btn btn-sm"
                >
                  Yes, Delete My Account
                </button>
                <button onClick={() => setShowDeleteConfirm(false)} className="btn-secondary btn btn-sm">
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </AppLayout>
  )
}
