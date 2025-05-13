import type { Path } from './Path'
import type { Stop } from './Stop'

export interface Routes {
  stops: Record<string, Stop[]>
  paths: Record<string, Path[]>
  cost: number
}
