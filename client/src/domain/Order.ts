import type { Location } from './Location'

export interface Order {
  id: string
  clientId: string // Added field
  location: Location
  requestedGLP: number
  deliveredGLP: number
  date: string // ISO date-time string
  limitTime: string // ISO date-time string
  maxDeliveryDate: string // ISO date-time string
  status: 'PENDING' | 'CALCULATING' | 'IN_PROGRESS' | 'COMPLETED'
}
