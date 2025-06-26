import { create } from 'zustand'

type SimulationStore = {
  executionMode: 'simulation' | 'real'
  setExecutionMode: (mode: 'simulation' | 'real') => void
}

export const useSimulationStore = create<SimulationStore>((set) => ({
  executionMode: 'simulation',
  setExecutionMode: (mode) => set({ executionMode: mode }),
}))