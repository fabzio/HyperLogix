import { cleanEmptyParams } from '@/lib/cleanEmptyParams'
import {
  type RegisteredRouter,
  type RouteIds,
  getRouteApi,
  useNavigate,
} from '@tanstack/react-router'

export function useFilters<T extends RouteIds<RegisteredRouter['routeTree']>>(
  routeId: T,
) {
  const routeApi = getRouteApi<T>(routeId)
  const navigate = useNavigate()
  const filters = routeApi.useSearch()

  const setFilters = (partialFilters: Partial<typeof filters>) =>
    navigate({
      //@ts-expect-error
      search: (prev) => cleanEmptyParams({ ...prev, ...partialFilters }),
      strict: true,
    })
  const resetFilters = () =>
    navigate({
      //@ts-expect-error
      search: {},
      strict: true,
    })

  return { filters, setFilters, resetFilters }
}
