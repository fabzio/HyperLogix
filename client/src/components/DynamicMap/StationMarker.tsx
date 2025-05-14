import StationIcon from '@/components/icons/StationIcon'
import type { Station } from '@/domain/Station'
import { memo } from 'react'
import { Marker } from 'react-simple-maps'
import { formatTooltipText } from './index'

const StationMarker = memo(
  ({
    station,
    cx,
    cy,
  }: {
    station: Station
    cx: number
    cy: number
  }) => (
    <Marker coordinates={[cx - 2.5, cy - 2]}>
      <StationIcon />
      <title>{formatTooltipText(station)}</title>
    </Marker>
  ),
)

export default StationMarker
