import type { Station } from '@/domain/Station'
import { Building2, Fuel } from 'lucide-react'
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
      {station.mainStation ? <Building2 size={5} /> : <Fuel size={5} />}
      <title>{formatTooltipText(station)}</title>
    </Marker>
  ),
)

export default StationMarker
