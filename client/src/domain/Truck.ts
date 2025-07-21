import type { Location } from './Location'
import type { TruckState } from './TruckState'
import type { TruckType } from './TruckType'

export interface Truck {
  id: string
  code: string
  type: TruckType
  status: TruckState
  tareWeight: number
  maxCapacity: number
  currentCapacity: number // Replaces currentGLP for consistency with Java; currentGLP might be an alias or derived
  fuelCapacity: number
  currentFuel: number
  nextMaintenance?: string // ISO date-time string
  location: Location
}
