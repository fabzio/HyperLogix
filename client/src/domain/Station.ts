import type { Location } from './Location'

export interface Station {
  id: string
  location: Location
  currentGLP: number
}
