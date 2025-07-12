import { SearchFilter } from '@/components/SearchFilter'
import { useFilters } from '@/hooks/useFilters'
import { useRouter } from '@tanstack/react-router'

export default function SearchInput() {
  const router = useRouter()
  const routeId = router.state.location.pathname as
    | '/_auth/trucks'
    | '/_auth/stations'
  const { filters, setFilters } = useFilters(routeId)

  const searchValue =
    typeof filters === 'object' &&
    'search' in filters &&
    typeof filters.search === 'string'
      ? filters.search
      : ''

  return (
    <div className="w-full md:w-96">
      <SearchFilter
        placeholder="Buscar..."
        value={searchValue}
        onChange={(value) => setFilters({ ...filters, search: value })}
      />
    </div>
  )
}
