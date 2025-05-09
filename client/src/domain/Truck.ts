import type { Location } from './Location'

export interface Truck {
  id: string
  location: Location
  type: string
  currentGLP: number
  currentFuel: number
}
