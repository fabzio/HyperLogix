import Simulation from '@/features/simulation'
import { getSimulationStatus } from '@/services/SimulatorService'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/simulacion')({
  validateSearch: () =>
    ({}) as {
      truckId: number | undefined
    },
  loader: async ({ context: { queryClient } }) =>
    await queryClient.ensureQueryData({
      queryKey: ['simulation'],
      queryFn: () => getSimulationStatus('fabzio'),
    }),
  component: RouteComponent,
})

function RouteComponent() {
  return <Simulation />
}
