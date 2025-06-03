import type { PLGNetwork } from '@/domain/PLGNetwork'
import type { Routes } from '@/domain/Routes'
import { create } from 'zustand'

export interface SimulationStore {
  plgNetwork: PLGNetwork | null
  simulationTime: string | null
  routes: Routes | null
  setState: (state: Partial<SimulationStore> | null) => void
}

export const useSimulationStore = create<SimulationStore>((set) => ({
  plgNetwork: null,
  simulationTime: null,
  routes: null,

  setState: (state) => set((prev) => ({ ...prev, ...state })),
}))
