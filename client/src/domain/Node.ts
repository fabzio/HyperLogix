import type { Location } from './Location'
import type { NodeType } from './NodeType'

export interface Node {
  id: string
  name: string
  type: NodeType
  location: Location
}
