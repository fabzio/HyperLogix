import OperationMap from '@/features/dashboard/OperationMap'
import { createFileRoute } from '@tanstack/react-router'
import { z } from 'zod'

const mapSearchSchema = z.object({
  truckId: z.number().optional(),
  stationId: z.string().optional(),
  orderId: z.string().optional(),
  roadblockStart: z.string().optional(),
})

export const Route = createFileRoute('/_auth/map')({
  component: RouteComponent,
  validateSearch: mapSearchSchema,
})

function RouteComponent() {
  return <OperationMap />
}
