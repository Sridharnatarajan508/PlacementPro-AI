import { useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Menu } from 'lucide-react'
import Sidebar from './Sidebar'

const PAGE_TITLES = {
  '/dashboard': 'Dashboard',
  '/resume': 'Resume Analyzer',
  '/jobs': 'Job Matcher',
  '/skill-gap': 'Skill Gap Analyzer',
  '/roadmap': 'Career Roadmap',
  '/mock-interview': 'Mock Interview',
  '/preparation': 'Preparation Tracker',
  '/applications': 'Application Tracker',
  '/assistant': 'AI Career Assistant',
  '/profile': 'My Profile',
  '/settings': 'Settings',
}

export default function AppLayout({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const location = useLocation()
  const title = PAGE_TITLES[location.pathname] || 'Career Copilot'

  return (
    <div className="flex h-screen overflow-hidden bg-surface-50">
      {/* Desktop Sidebar */}
      <div className="hidden lg:flex lg:flex-shrink-0">
        <Sidebar />
      </div>

      {/* Mobile Sidebar Overlay */}
      {sidebarOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div
            className="fixed inset-0 bg-surface-900/50"
            onClick={() => setSidebarOpen(false)}
          />
          <div className="relative z-50 h-full">
            <Sidebar mobile onClose={() => setSidebarOpen(false)} />
          </div>
        </div>
      )}

      {/* Main content */}
      <div className="flex flex-col flex-1 min-w-0 overflow-hidden">
        {/* Top navbar */}
        <header className="flex items-center h-16 px-4 md:px-6 bg-white border-b border-surface-200 flex-shrink-0">
          <button
            onClick={() => setSidebarOpen(true)}
            className="btn-ghost p-2 rounded-md lg:hidden mr-2"
            aria-label="Open navigation"
          >
            <Menu className="h-5 w-5" />
          </button>
          <h1 className="text-base font-semibold text-surface-800">{title}</h1>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          <div className="p-4 md:p-6 max-w-7xl mx-auto animate-fade-in">
            {children}
          </div>
        </main>
      </div>
    </div>
  )
}
