import { Loader2 } from 'lucide-react'

export default function LoadingSpinner({ size = 'md', text, fullScreen = false }) {
  const sizes = {
    sm: 'h-4 w-4',
    md: 'h-6 w-6',
    lg: 'h-8 w-8',
    xl: 'h-12 w-12',
  }

  if (fullScreen) {
    return (
      <div className="fixed inset-0 flex flex-col items-center justify-center bg-white/80 z-50">
        <Loader2 className={`${sizes[size]} text-primary-600 animate-spin`} />
        {text && <p className="mt-3 text-sm text-surface-500">{text}</p>}
      </div>
    )
  }

  return (
    <div className="flex flex-col items-center justify-center py-8">
      <Loader2 className={`${sizes[size]} text-primary-600 animate-spin`} />
      {text && <p className="mt-2 text-sm text-surface-500">{text}</p>}
    </div>
  )
}
