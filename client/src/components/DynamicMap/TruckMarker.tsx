import TruckIcon from '@/components/icons/TruckIcon'
import type { Truck } from '@/domain/Truck'
import { memo } from 'react'
import { Marker } from 'react-simple-maps'
import { formatTooltipText } from './index'

const TruckMarker = memo(
  ({
    truck,
    cx,
    cy,
  }: {
    truck: Truck
    cx: number
    cy: number
  }) => (
    <Marker coordinates={[cx - 2, cy - 2]} onClick={() => console.log(truck)}>
      <TruckIcon />
      <title>{formatTooltipText(truck)}</title>
    </Marker>
  ),
)

export default TruckMarker
