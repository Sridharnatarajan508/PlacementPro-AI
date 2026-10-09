# AI Career & Placement Copilot

## Overview

An AI-powered career and placement platform designed for college students and fresh graduates. Acts as a personal AI career assistant helping users analyze resumes, match jobs, identify skill gaps, generate career roadmaps, practice mock interviews, and track job applications.

## Features

- 🤖 **AI Resume Analyzer (Phase 3 Active)** — Google Gemini AI analysis featuring overall & ATS compatibility scoring, executive summary, strengths & weaknesses, categorized skill detection, missing skills with priority ratings, actionable suggestions, and section-by-section feedback.
- 📄 **PDF Text Extraction (Phase 2 Active)** — Apache PDFBox text extraction from uploaded PDF resumes.
- 🔐 **Secure Authentication (Phase 1 Active)** — JWT (jjwt 0.12.x) + BCrypt, MySQL persistence, protected routes, data isolation.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite, Tailwind CSS 3, React Router v7, Axios, Recharts, React Hot Toast, Lucide Icons |
| Backend | Java 22, Spring Boot 3.2, Spring Security, Spring Data JPA, Apache PDFBox 3.0 |
| Database | MySQL 8+ |
| Authentication | JWT (jjwt 0.12.x), BCrypt password hashing |
| AI Integration | Google Gemini API (via Spring Boot backend) |
| API Docs | Springdoc OpenAPI / Swagger UI |

## Architecture & Data Flow

```
React Frontend (port 5173)
    ↓ Axios + JWT
Spring Boot Backend (port 8080)
    ↓ Spring Data JPA
MySQL (port 3306)

AI Analysis Flow:
PDF Upload → Apache PDFBox Text Extraction → GeminiService (Backend) → Gemini API (REST) → Structured JSON → MySQL ResumeAnalysis → React UI
```

> 🔒 **Security Guarantee**: The `GEMINI_API_KEY` is loaded ONLY on the Spring Boot backend via environment variable. The frontend never accesses or exposes the API key.

## Project Structure

```
ai-career-copilot/
├── backend/
│   ├── pom.xml
│   ├── .env.example
│   └── src/main/java/com/careerpilot/app/
│       ├── CareerCopilotApplication.java
│       ├── config/          (SecurityConfig, JpaConfig)
│       ├── controller/      (AuthController, ProfileController, ResumeController)
│       ├── service/         (AuthService, ProfileService, ResumeService, ResumeAnalysisService, GeminiService)
│       ├── repository/      (UserRepository, ProfileRepository, ResumeRepository, ResumeAnalysisRepository)
│       ├── entity/          (User, Profile, Resume, ResumeAnalysis)
│       ├── dto/             (request/, response/)
│       ├── security/        (JwtService, JwtAuthFilter, UserDetailsServiceImpl)
│       └── exception/       (GlobalExceptionHandler, ApiError, AIServiceException, ...)
└── frontend/
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    └── src/
        ├── App.jsx
        ├── context/         (AuthContext.jsx)
        ├── services/        (apiClient.js, api.js)
        ├── components/
        │   ├── auth/        (ProtectedRoute, PublicRoute)
        │   ├── layout/      (Sidebar, AppLayout)
        │   └── ui/          (LoadingSpinner, EmptyState)
        └── pages/
            ├── LandingPage.jsx
            ├── LoginPage.jsx
            ├── RegisterPage.jsx
            ├── OnboardingPage.jsx
            ├── DashboardPage.jsx
            ├── ProfilePage.jsx
            ├── ResumePage.jsx (Full Upload + PDFBox + Gemini AI Analysis UI)
            └── SettingsPage.jsx
```

## Setup Instructions

### Prerequisites

- Java 17+ (tested with Java 22)
- Node.js 18+ (tested with Node 22)
- Maven 3.9+
- MySQL 8.0+
- Google Gemini API key (get one from https://aistudio.google.com/)

## Environment Variables

### Backend — `backend/.env`

```properties
# MySQL Configuration
DB_URL=jdbc:mysql://localhost:3306/career_copilot_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# JWT Configuration
JWT_SECRET=your_strong_base64_encoded_secret_at_least_32_bytes
JWT_EXPIRATION=86400000

# Gemini AI Integration (Required for Phase 3)
GEMINI_API_KEY=your_google_gemini_api_key_here
GEMINI_MODEL=gemini-1.5-flash
```

> ⚠️ Never commit real secrets. The `.env` file is git-ignored.

## Running Backend

```powershell
cd backend

# Set environment variables in PowerShell
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
$env:GEMINI_API_KEY = "your_gemini_api_key_here"

# Run Spring Boot app
mvn spring-boot:run
```

Backend: **http://localhost:8080**
Swagger UI: **http://localhost:8080/swagger-ui.html**

## Running Frontend

```powershell
cd frontend
npm install
npm run dev
```

Frontend: **http://localhost:5173**

## API Documentation (Phase 3)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/auth/register` | No | Register new user |
| POST | `/api/auth/login` | No | Login & receive JWT |
| GET | `/api/profile` | Yes | Get profile |
| PUT | `/api/profile` | Yes | Update profile |
| POST | `/api/resumes/upload` | Yes | Upload PDF resume & extract text with PDFBox |
| GET | `/api/resumes/latest` | Yes | Get latest uploaded resume |
| DELETE | `/api/resumes/{id}` | Yes | Delete resume & file |
| POST | `/api/resumes/{id}/analyze` | Yes | Trigger/re-run Gemini AI Resume Analysis |
| GET | `/api/resumes/{id}/analysis` | Yes | Get stored AI analysis (no Gemini call if cached) |
| DELETE | `/api/resumes/{id}/analysis` | Yes | Delete stored AI analysis |

## Testing

```powershell
# Run backend tests (includes Resume & Gemini AI analysis service tests)
cd backend
mvn test
```
Result: **Tests run: 11, Failures: 0, Errors: 0, Skipped: 0** (`BUILD SUCCESS`)

```powershell
# Run frontend build verification
cd frontend
npm run build
```
Result: **✓ 1890 modules transformed** in 6.92s (`BUILD SUCCESS`)

## Future Enhancements (Phase 4+)

- [ ] Phase 7: Job Matcher + AI scoring
- [ ] Phase 8: Skill Gap Analyzer
- [ ] Phase 9: Career Roadmap generator
- [ ] Phase 10: Mock Interview with AI evaluation
- [ ] Phase 11: Preparation Tracker
- [ ] Phase 12: Application Tracker (full CRUD)
- [ ] Phase 13: AI Career Assistant (contextual chatbot)
- [ ] Phase 14: Analytics dashboard with Recharts
"# PlacementPro-AI" 
