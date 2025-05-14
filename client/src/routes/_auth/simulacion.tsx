import Simulation from '@/features/simulation'
import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_auth/simulacion')({
  component: RouteComponent,
})

function RouteComponent() {
  return <Simulation />
}
