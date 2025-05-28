import type { Order } from './Order'
import type { Station } from './Station'
import type { Truck } from './Truck'

interface Incident {
  id: string
}

interface Roadblock {
  id: string
}

export interface PLGNetwork {
  trucks: Truck[]
  stations: Station[]
  orders: Order[]
  incidents: Incident[]
  roadblocks: Roadblock[]
}
