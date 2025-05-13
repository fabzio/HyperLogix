import type { Node } from './Node' // Or a more specific Stop related Node information

export interface Stop {
  // Example properties, adjust based on actual Stop.java definition
  node: Node // Reference to the node being stopped at
  arrivalTime?: string // ISO date-time string
  departureTime?: string // ISO date-time string
  serviceTimeMinutes?: number
  loadChange?: number // e.g., amount of GLP loaded/unloaded
  // Add other properties as defined in your Java Stop class
}
