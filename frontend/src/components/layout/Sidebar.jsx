import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import {
  LayoutDashboard, FileText, Briefcase, Zap, Map, Mic,
  BookOpen, ClipboardList, MessageSquare, User, Settings,
  LogOut, Bot, ChevronLeft, X, TrendingUp
} from 'lucide-react'

const NAV_ITEMS = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/resume', icon: FileText, label: 'Resume Analyzer' },
  { to: '/jobs', icon: Briefcase, label: 'Job Matcher' },
  { to: '/skill-gap', icon: Zap, label: 'Skill Gap' },
  { to: '/roadmap', icon: Map, label: 'Career Roadmap' },
  { to: '/mock-interview', icon: Mic, label: 'Mock Interview' },
  { to: '/preparation', icon: BookOpen, label: 'Preparation' },
  { to: '/career-analytics', icon: TrendingUp, label: 'Career Analytics' },
  { to: '/applications', icon: ClipboardList, label: 'Applications' },
  { to: '/assistant', icon: MessageSquare, label: 'AI Assistant' },
]

const BOTTOM_ITEMS = [
  { to: '/profile', icon: User, label: 'Profile' },
  { to: '/settings', icon: Settings, label: 'Settings' },
]

export default function Sidebar({ mobile = false, onClose }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <aside className="flex flex-col h-full bg-white border-r border-surface-200 w-64">
      {/* Logo */}
      <div className="flex items-center justify-between h-16 px-5 border-b border-surface-200 flex-shrink-0">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-primary-600 flex items-center justify-center">
            <Bot className="h-4.5 w-4.5 text-white h-5 w-5" />
          </div>
          <span className="text-sm font-bold text-surface-900">Career Copilot</span>
        </div>
        {mobile && (
          <button onClick={onClose} className="btn-ghost p-1 rounded-md">
            <X className="h-5 w-5" />
          </button>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-4 px-3 space-y-0.5">
        {NAV_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={mobile ? onClose : undefined}
            className={({ isActive }) =>
              isActive ? 'sidebar-link-active' : 'sidebar-link-inactive'
            }
          >
            <Icon className="h-4 w-4 flex-shrink-0" />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Bottom section */}
      <div className="border-t border-surface-200 py-3 px-3 space-y-0.5">
        {BOTTOM_ITEMS.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            onClick={mobile ? onClose : undefined}
            className={({ isActive }) =>
              isActive ? 'sidebar-link-active' : 'sidebar-link-inactive'
            }
          >
            <Icon className="h-4 w-4 flex-shrink-0" />
            <span>{label}</span>
          </NavLink>
        ))}

        {/* User info + Logout */}
        <div className="pt-2 mt-1 border-t border-surface-100">
          <div className="flex items-center gap-2.5 px-3 py-2 mb-1">
            <div className="w-7 h-7 rounded-full bg-primary-100 flex items-center justify-center flex-shrink-0">
              <span className="text-xs font-semibold text-primary-700">
                {user?.name?.charAt(0)?.toUpperCase()}
              </span>
            </div>
            <div className="min-w-0">
              <p className="text-xs font-semibold text-surface-800 truncate">{user?.name}</p>
              <p className="text-xs text-surface-400 truncate">{user?.email}</p>
            </div>
          </div>
          <button
            onClick={handleLogout}
            className="sidebar-link-inactive w-full text-danger-600 hover:bg-danger-50 hover:text-danger-700"
          >
            <LogOut className="h-4 w-4 flex-shrink-0" />
            <span>Logout</span>
          </button>
        </div>
      </div>
    </aside>
  )
}
