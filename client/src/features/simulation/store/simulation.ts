import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { create } from 'zustand'

type SimulationMetrics = {
  fleetUtilizationPercentage: number

  averageFuelConsumptionPerKm: number

  completionPercentage: number
  averageDeliveryTimeMinutes: number

  averageCapacityUtilization: number

  averagePlanificationTimeSeconds: number

  totalDistanceTraveled: number

  deliveryEfficiencyPercentage: number
}
export interface SimulationStore {
  plgNetwork: PLGNetwork | null
  simulationTime: string | null
  routes: Routes | null
  metrics: SimulationMetrics | null

  setState: (state: Partial<SimulationStore> | null) => void
}

export const useSimulationStore = create<SimulationStore>((set) => ({
  plgNetwork: null,
  simulationTime: null,
  routes: null,
  metrics: null,
  setState: (state) => set((prev) => ({ ...prev, ...state })),
}))
