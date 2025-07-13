import { SearchFilter } from '@/components/SearchFilter'
import { useFilters } from '@/hooks/useFilters'
import { useRouter } from '@tanstack/react-router'

export default function SearchInput() {
  const router = useRouter()

  // Get the current pathname and handle route matching more safely
  const pathname = router.state.location.pathname

  // Extract the route ID more safely, handling both full paths and relative paths
  const getRouteId = (path: string): '/_auth/trucks' | '/_auth/stations' => {
    if (path.includes('/trucks')) {
      return '/_auth/trucks'
    }if (path.includes('/stations')) {
      return '/_auth/stations'
    }
    return '/_auth/trucks'
  }

  const routeId = getRouteId(pathname)
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
