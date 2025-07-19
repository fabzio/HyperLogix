import Simulation from '@/features/simulation'
import { getSimulationStatus } from '@/services/SimulatorService'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/simulacion')({
  validateSearch: () =>
    ({}) as {
      truckId?: number
      orderId?: string
      stationId?: string
      roadblockStart?: string
    },
  loader: async ({ context: { queryClient } }) => {
    // Get user from session store - this would normally be passed from a user context
    // For now, we'll use a default simulation ID - this should be replaced with actual user logic
    const simulationId = 'main' // This should come from user session
    return await queryClient.ensureQueryData({
      queryKey: ['simulation', simulationId],
      queryFn: () => getSimulationStatus(simulationId),
    })
  },
  component: RouteComponent,
})

function RouteComponent() {
  return <Simulation />
}
