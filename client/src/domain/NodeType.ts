export const NodeType = {
  LOCATION: 'LOCATION',
  STATION: 'STATION',
  DELIVERY: 'DELIVERY',
} as const

export type NodeType = (typeof NodeType)[keyof typeof NodeType]
