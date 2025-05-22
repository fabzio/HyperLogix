import QueryKeys from '@/const/QueryKeys'
import TrucksFeature from '@/features/trucks'
import { listTrucks } from '@/services/TruckService'
import type { Filters } from '@/types/Params'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/trucks')({
  loader: async ({ context: { queryClient } }) =>
    await queryClient.ensureQueryData({
      queryKey: [QueryKeys.TRUCKS, {}],
      queryFn: () => listTrucks(),
    }),
  validateSearch: () => ({}) as Filters,
  component: TrucksFeature,
})
