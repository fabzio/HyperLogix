import QueryKeys from '@/const/QueryKeys'
import { listStations } from '@/services/StationService'
import { useQuery } from '@tanstack/react-query'

export function useStations(page = 0, size = 20) {
  return useQuery({
    queryKey: [QueryKeys.STATIONS],
    queryFn: () => listStations(page, size),
  })
}
