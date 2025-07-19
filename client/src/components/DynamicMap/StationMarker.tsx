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
    onClick,
  }: {
    station: Station
    cx: number
    cy: number
    onClick?: (stationId: string) => void
  }) => (
    <Marker
      coordinates={[cx - 2.5, cy - 2]}
      onClick={() => onClick?.(station.id)}
      style={{
        default: { cursor: onClick ? 'pointer' : 'default' },
        hover: { cursor: onClick ? 'pointer' : 'default' },
      }}
    >
      {/* Invisible larger clickable area */}
      {onClick && (
        <circle
          r="8"
          fill="transparent"
          stroke="transparent"
          style={{ cursor: 'pointer' }}
        />
      )}
      {/* Icono más pequeño */}
      <g>
        {station.mainStation ? <Building2 size={6} /> : <Fuel size={6} />}
        {/* Texto con el nombre debajo del icono */}
        <text
          x={0}
          y={12}
          textAnchor="middle"
          fontSize="3"
          fill="currentColor"
          fontWeight="500"
        >
          {station.name}
        </text>
      </g>
      <title>{formatTooltipText(station)}</title>
    </Marker>
  ),
)

export default StationMarker
