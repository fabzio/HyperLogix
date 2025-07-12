import type { Location } from './Location'

export interface Station {
  id: string
  name: string
  location: Location
  maxCapacity: number
  mainStation: boolean
  availableCapacityPerDate: Record<string, number> // Key: ISO date string (YYYY-MM-DD), Value: capacity
  reservationHistory: Array<{
    dateTime: string // ISO date string with time
    amount: number
    vehicleId: string
    orderId: string
  }>
}
