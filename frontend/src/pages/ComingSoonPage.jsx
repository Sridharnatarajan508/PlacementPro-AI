import AppLayout from '../components/layout/AppLayout'
import { Construction } from 'lucide-react'

/**
 * Placeholder page for features not yet implemented.
 * Replace this component when implementing the actual feature.
 */
export default function ComingSoonPage({ title, description, phase }) {
  return (
    <AppLayout>
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center px-4">
        <div className="w-16 h-16 rounded-2xl bg-amber-50 flex items-center justify-center mb-5">
          <Construction className="h-8 w-8 text-amber-500" />
        </div>
        <h2 className="text-xl font-bold text-surface-900 mb-2">{title}</h2>
        <p className="text-surface-400 text-sm max-w-sm leading-relaxed">
          {description || 'This feature is coming in a future phase. The backend API is ready for integration.'}
        </p>
        {phase && (
          <span className="badge-warning mt-4">Phase {phase}</span>
        )}
      </div>
    </AppLayout>
  )
}
