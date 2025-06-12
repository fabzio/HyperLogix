import type { Truck as TruckType } from '@/domain/Truck'
import { Truck } from 'lucide-react'
import { memo } from 'react'
import { Marker } from 'react-simple-maps'
import { formatTooltipText } from './index'

const TruckMarker = memo(
  ({
    truck,
    cx,
    cy,
  }: {
    truck: TruckType
    cx: number
    cy: number
  }) => (
    <Marker coordinates={[cx - 2, cy - 2]}>
      <Truck size={5} />
      <title>{formatTooltipText(truck)}</title>
    </Marker>
  ),
)

export default TruckMarker
