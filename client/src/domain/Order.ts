import type { Location } from './Location'

export interface Order {
  id: string
  clientId: string // Added field
  location: Location
  GLPrequested: number
  GLPDelivered: number
  requestedDate: string // ISO date-time string
  limitTime: string // ISO date-time string
}
