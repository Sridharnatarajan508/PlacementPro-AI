import apiClient from './apiClient'

export const authService = {
  register: (data) => apiClient.post('/auth/register', data),
  login: (data) => apiClient.post('/auth/login', data),
}

export const profileService = {
  getProfile: () => apiClient.get('/profile'),
  updateProfile: (data) => apiClient.put('/profile', data),
}

export const resumeService = {
  uploadResume: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/resumes/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  getResumes: () => apiClient.get('/resumes'),
  getLatestResume: () => apiClient.get('/resumes/latest'),
  getResumeById: (id) => apiClient.get(`/resumes/${id}`),
  deleteResume: (id) => apiClient.delete(`/resumes/${id}`),
  analyzeResume: (id) => apiClient.post(`/resumes/${id}/analyze`),
  getResumeAnalysis: (id) => apiClient.get(`/resumes/${id}/analysis`),
  deleteResumeAnalysis: (id) => apiClient.delete(`/resumes/${id}/analysis`),
}

export const jobService = {
  getJobs: (params) => apiClient.get('/jobs', { params }),
  getJobById: (id) => apiClient.get(`/jobs/${id}`),
  getRecommendedJobs: () => apiClient.get('/jobs/recommended'),
  getSavedJobs: () => apiClient.get('/jobs/saved'),
  matchJob: (id) => apiClient.post(`/jobs/${id}/match`),
  getMatchForJob: (id) => apiClient.get(`/jobs/${id}/match`),
  saveJob: (id) => apiClient.post(`/jobs/${id}/save`),
  unsaveJob: (id) => apiClient.delete(`/jobs/${id}/save`),
}

export const skillGapService = {
  analyzeSkillGap: (data) => apiClient.post('/skill-gaps/analyze', data),
  getLatestSkillGap: () => apiClient.get('/skill-gaps/latest'),
  getSkillGapHistory: () => apiClient.get('/skill-gaps/history'),
  deleteSkillGap: (id) => apiClient.delete(`/skill-gaps/${id}`),
}

export const careerRoadmapService = {
  getLatestRoadmap: () => apiClient.get('/career-roadmap'),
  generateRoadmap: () => apiClient.post('/career-roadmap/generate'),
  regenerateRoadmap: () => apiClient.post('/career-roadmap/regenerate'),
  getLearningRecommendations: () => apiClient.get('/career-roadmap/learning'),
  getRoadmaps: () => apiClient.get('/career-roadmaps/all'),
  getRoadmapById: (id) => apiClient.get(`/career-roadmaps/${id}`),
  deleteRoadmap: (id) => apiClient.delete(`/career-roadmaps/${id}`),
  completeTask: (roadmapId, taskId) => apiClient.post(`/career-roadmaps/${roadmapId}/tasks/${taskId}/complete`),
  uncompleteTask: (roadmapId, taskId) => apiClient.post(`/career-roadmaps/${roadmapId}/tasks/${taskId}/uncomplete`),
  getProgress: (id) => apiClient.get(`/career-roadmaps/${id}/progress`),
}

export const jobPreparationService = {
  getAll: () => apiClient.get('/job-preparation'),
  get: (jobId) => apiClient.get(`/job-preparation/${jobId}`),
  generate: (jobId) => apiClient.post(`/job-preparation/${jobId}/generate`),
  regenerate: (jobId) => apiClient.post(`/job-preparation/${jobId}/regenerate`),
  delete: (jobId) => apiClient.delete(`/job-preparation/${jobId}`),
}

export const interviewService = {
  startInterview: (data) => apiClient.post('/interviews/start', data),
  submitAnswer: (sessionId, questionId, data) => apiClient.post(`/interviews/${sessionId}/questions/${questionId}/answer`, data),
  getInterview: (sessionId) => apiClient.get(`/interviews/${sessionId}`),
  getInterviewHistory: () => apiClient.get('/interviews/history'),
  getNextQuestion: (sessionId) => apiClient.get(`/interviews/${sessionId}/next-question`),
  abandonInterview: (sessionId) => apiClient.post(`/interviews/${sessionId}/abandon`),
}

export const careerAssistantService = {
  getConversations: () => apiClient.get('/assistant/conversations'),
  getConversation: (id) => apiClient.get(`/assistant/conversations/${id}`),
  createConversation: (title = 'New Conversation') => apiClient.post('/assistant/conversations', { title }),
  sendMessage: (id, message) => apiClient.post(`/assistant/conversations/${id}/messages`, { message }),
  ask: (message) => apiClient.post('/assistant/ask', { message }),
  deleteConversation: (id) => apiClient.delete(`/assistant/conversations/${id}`),
  // Aliases for backwards compatibility
  getChats: () => apiClient.get('/assistant/conversations'),
  getChat: (chatId) => apiClient.get(`/assistant/conversations/${chatId}`),
  createChat: (title = 'New Conversation') => apiClient.post('/assistant/conversations', { title }),
  deleteChat: (chatId) => apiClient.delete(`/assistant/conversations/${chatId}`),
}

export const careerAnalyticsService = {
  getAnalytics: () => apiClient.get('/career-analytics'),
  generateAnalytics: () => apiClient.post('/career-analytics/generate'),
  regenerateAnalytics: () => apiClient.post('/career-analytics/regenerate'),
}

export const jobApplicationService = {
  getApplications: () => apiClient.get('/applications'),
  getApplication: (id) => apiClient.get(`/applications/${id}`),
  createApplication: (jobId) => apiClient.post(`/applications/job/${jobId}`),
  createCustomApplication: (data) => apiClient.post('/applications', data),
  updateApplication: (id, data) => apiClient.put(`/applications/${id}`, data),
  updateStatus: (id, status) => apiClient.patch(`/applications/${id}/status`, { status }),
  deleteApplication: (id) => apiClient.delete(`/applications/${id}`),
  getByStatus: (status) => apiClient.get(`/applications/status/${status}`),
  getStats: () => apiClient.get('/applications/stats'),
  getUpcoming: () => apiClient.get('/applications/upcoming'),
}

export const applicationIntelligenceService = {
  analyzeApplication: (id) => apiClient.get(`/applications/${id}/intelligence`),
  regenerateAnalysis: (id) => apiClient.post(`/applications/${id}/intelligence/regenerate`),
}




