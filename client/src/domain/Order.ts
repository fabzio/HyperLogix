import type { Location } from './Location'

export interface Order {
  id: string
  location: Location
  GLPrequested: number
  GLPDelivered: number
  requestedDate: string
  limitTime: string
}
