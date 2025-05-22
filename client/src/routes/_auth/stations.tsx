import QueryKeys from '@/const/QueryKeys'
import StationsFeature from '@/features/stations'
import { listStations } from '@/services/StationService'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/stations')({
  loader: async ({ context: { queryClient } }) =>
    await queryClient.ensureQueryData({
      queryKey: [QueryKeys.STATIONS],
      queryFn: () => listStations(),
    }),
  component: StationsFeature,
})
