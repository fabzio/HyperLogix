import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

export interface SessionStore {
  username: string | null
  setUsername: (username: string | null) => void
}

export const useSessionStore = create<SessionStore>()(
  persist(
    (set) => ({
      username: null,
      setUsername: (username: string | null) => set({ username }),
    }),
    {
      name: 'session-storage',
      storage: createJSONStorage(() => localStorage),
    },
  ),
)
