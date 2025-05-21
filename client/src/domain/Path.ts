import type { Location } from './Location'

export interface Path {
  // Example properties, adjust based on actual Path.java definition
  fromNodeId: string
  toNodeId: string
  distanceKm: number
  travelTimeMinutes: number
  points?: Location[] // Optional list of geographical points defining the path
  // Add other properties as defined in your Java Path class
}
