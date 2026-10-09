import { Navigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'

export default function PublicRoute({ children }) {
  const { isAuthenticated, user } = useAuth()

  if (isAuthenticated) {
    const dest = user?.onboardingComplete ? '/dashboard' : '/onboarding'
    return <Navigate to={dest} replace />
  }

  return children
}
