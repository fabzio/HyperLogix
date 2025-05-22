import QueryKeys from '@/const/QueryKeys'
import { listTrucks } from '@/services/TruckService'
import { keepPreviousData, useQuery } from '@tanstack/react-query'

export function useTrucks(filters: { pageIndex?: number; pageSize?: number }) {
  return useQuery({
    queryKey: [QueryKeys.TRUCKS, filters],
    queryFn: () => listTrucks(filters.pageIndex, filters.pageSize),
    placeholderData: keepPreviousData,
  })
}
