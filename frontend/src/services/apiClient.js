import axios from 'axios'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 75000,
})

// Attach JWT token to every request
apiClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('cp_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

let isHandlingUnauthorized = false

// Handle auth errors globally
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const requestUrl = error.config?.url || ''
      const isLoginRequest = requestUrl.includes('/auth/login')

      if (!isLoginRequest) {
        // Token expired or invalid — clear stale auth state
        localStorage.removeItem('cp_token')
        localStorage.removeItem('cp_user')

        // Notify in-memory AuthContext
        window.dispatchEvent(new CustomEvent('auth:unauthorized'))

        // Redirect to login if not already there, debouncing to prevent infinite loops
        const currentPath = window.location.pathname
        if (!isHandlingUnauthorized && currentPath !== '/login' && currentPath !== '/register') {
          isHandlingUnauthorized = true
          setTimeout(() => {
            window.location.href = '/login'
          }, 50)
        }
      }
    }
    return Promise.reject(error)
  }
)

// Helper to extract error message from API response
export function getErrorMessage(error) {
  if (!error) return 'An unexpected error occurred.'
  if (error.response?.status === 429) {
    const retryAfter = error.response.data?.retryAfterSeconds || error.response.headers?.['retry-after']
    if (retryAfter && Number(retryAfter) > 0) {
      return `Gemini usage limit reached. Please wait ${retryAfter} seconds and try again.`
    }
    if (error.response?.data?.message) {
      return error.response.data.message
    }
    return 'Gemini usage limit reached. Please wait and try again later.'
  }
  if (error.response?.data?.message) return error.response.data.message
  if (error.response?.status === 504) return 'AI service timed out. Please try again.'
  if (error.response?.status === 502) return 'AI service is temporarily unavailable. Please try again.'
  if (error.code === 'ECONNABORTED' || error.message?.toLowerCase().includes('timeout')) {
    return 'Request timed out. Please try again.'
  }
  if (error.message === 'Network Error') return 'Unable to connect to the server. Please check your connection.'
  return 'Something went wrong. Please try again.'
}

export default apiClient
