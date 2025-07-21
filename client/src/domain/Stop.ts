import type { Node } from './Node' // Or a more specific Stop related Node information

export interface Stop {
  // Example properties, adjust based on actual Stop.java definition
  node: Node // Reference to the node being stopped at
  arrivalTime?: string // ISO date-time string
  arrived: boolean // Indicates if the stop has been completed
}
