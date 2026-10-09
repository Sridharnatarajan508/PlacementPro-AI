import { Search, MapPin, Briefcase, GraduationCap, X, Filter } from 'lucide-react'

export default function JobFilters({ filters, setFilters, onApply, onReset }) {
  const handleChange = (field, value) => {
    setFilters(prev => ({ ...prev, [field]: value }))
  }

  const hasActiveFilters = Boolean(
    filters.keyword || filters.location || (filters.jobType && filters.jobType !== 'ALL') ||
    (filters.experienceLevel && filters.experienceLevel !== 'ALL') || (filters.minimumMatchScore && filters.minimumMatchScore > 0)
  )

  return (
    <div className="bg-white rounded-xl border border-surface-200 p-4 sm:p-5 shadow-sm space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2 text-surface-800 font-semibold text-sm">
          <Filter className="h-4 w-4 text-primary-600" />
          <span>Filter & Search Jobs</span>
        </div>
        {hasActiveFilters && (
          <button
            onClick={onReset}
            className="text-xs text-primary-600 hover:text-primary-700 font-medium flex items-center gap-1 hover:underline"
          >
            <X className="h-3.5 w-3.5" />
            Clear all filters
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Keyword */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
          <input
            type="text"
            placeholder="Title, skills, company..."
            value={filters.keyword || ''}
            onChange={(e) => handleChange('keyword', e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onApply?.()}
            className="input pl-9 text-xs w-full"
          />
        </div>

        {/* Location */}
        <div className="relative">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
          <input
            type="text"
            placeholder="Location (e.g. Bengaluru, Remote)..."
            value={filters.location || ''}
            onChange={(e) => handleChange('location', e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && onApply?.()}
            className="input pl-9 text-xs w-full"
          />
        </div>

        {/* Job Type */}
        <div className="relative">
          <Briefcase className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
          <select
            value={filters.jobType || 'ALL'}
            onChange={(e) => handleChange('jobType', e.target.value)}
            className="input pl-9 text-xs w-full"
          >
            <option value="ALL">All Job Types</option>
            <option value="FULL_TIME">Full Time</option>
            <option value="INTERNSHIP">Internship</option>
            <option value="CONTRACT">Contract</option>
            <option value="PART_TIME">Part Time</option>
          </select>
        </div>

        {/* Experience Level */}
        <div className="relative">
          <GraduationCap className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-surface-400" />
          <select
            value={filters.experienceLevel || 'ALL'}
            onChange={(e) => handleChange('experienceLevel', e.target.value)}
            className="input pl-9 text-xs w-full"
          >
            <option value="ALL">All Experience Levels</option>
            <option value="FRESHER">Fresher / Entry Level</option>
            <option value="JUNIOR">Junior (1-2 yrs)</option>
            <option value="MID">Mid Level (3-5 yrs)</option>
            <option value="SENIOR">Senior (5+ yrs)</option>
          </select>
        </div>
      </div>
    </div>
  )
}
