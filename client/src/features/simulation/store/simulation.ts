import type { PLGNetwork } from '@/domain/PLGNetwork'
import { create } from 'zustand'

export interface SimulationStore {
  plgNetwork: PLGNetwork | null
  simulationTime: string | null
  setState: (state: Partial<SimulationStore> | null) => void
}

export const useSimulationStore = create<SimulationStore>((set) => ({
  plgNetwork: null,
  simulationTime: null,

  setState: (state) => set((prev) => ({ ...prev, ...state })),
}))
