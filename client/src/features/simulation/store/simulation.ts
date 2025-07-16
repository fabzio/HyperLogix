import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { create } from 'zustand'

type PlanificationStatus = {
  planning: boolean
  currentNodesProcessed: number
}

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
  planificationStatus: PlanificationStatus | null
  simulationTime: string | null
  routes: Routes | null
  metrics: SimulationMetrics | null
  finalMetrics: SimulationMetrics | null
  finalPlgNetwork: PLGNetwork | null
  simulationStartTime: string | null
  simulationEndTime: string | null
  collapseDetected: boolean
  collapseInfo: { type: string; description: string } | null

  setState: (state: Partial<SimulationStore> | null) => void
  saveFinalMetrics: (
    metrics: SimulationMetrics,
    endTime: string,
    plgNetwork?: PLGNetwork,
  ) => void
  setCollapseDetected: (collapseInfo: {
    type: string
    description: string
  }) => void
}

export const useSimulationStore = create<SimulationStore>((set) => ({
  plgNetwork: null,
  planificationStatus: null,
  simulationTime: null,
  routes: null,
  metrics: null,
  finalMetrics: null,
  finalPlgNetwork: null,
  simulationStartTime: null,
  simulationEndTime: null,
  collapseDetected: false,
  collapseInfo: null,
  setState: (state) => set((prev) => ({ ...prev, ...state })),
  saveFinalMetrics: (metrics, endTime, plgNetwork) =>
    set((prev) => ({
      ...prev,
      finalMetrics: metrics,
      simulationEndTime: endTime,
      finalPlgNetwork: plgNetwork || prev.plgNetwork,
    })),
  setCollapseDetected: (collapseInfo) =>
    set(() => ({
      collapseDetected: true,
      collapseInfo,
    })),
}))
