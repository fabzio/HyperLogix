import type { Order } from './Order'
import type { Station } from './Station'
import type { Truck } from './Truck'
import type { Point } from '../api'

interface Incident {
  id: string
}

interface Roadblock {
  start: string // ISO date-time string
  end: string // ISO date-time string
  blockedNodes: Point[]
}

export interface PLGNetwork {
  trucks: Truck[]
  stations: Station[]
  orders: Order[]
  incidents: Incident[]
  roadblocks: Roadblock[]
}
