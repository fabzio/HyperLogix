import type { Location } from '@/domain/Location'
import type { Order } from '@/domain/Order'
import type { Station } from '@/domain/Station'
import type { Truck } from '@/domain/Truck'
import { geoIdentity } from 'd3-geo'
import { useCallback, useMemo, useState } from 'react'
import {
  ComposableMap,
  Line,
  Marker,
  type ProjectionFunction,
  ZoomableGroup,
} from 'react-simple-maps'
import styles from './DynamicMap.module.css'

import { useTheme } from '../theme-provider'
import OrderMarker from './OrderMarker'
import PolylineItem from './PolylineItem'
import StationMarker from './StationMarker'
// Importa los componentes extraídos
import TruckMarker from './TruckMarker'

export interface MapPolyline {
  id: string
  points: [number, number][]
  stroke?: string
  strokeWidth?: number
  type?: 'path' | 'roadblock'
  startTime?: string // ISO date-time string
  endTime?: string // ISO date-time string
}

interface DynamicMapProps {
  points?: [number, number][]
  trucks?: Truck[]
  stations?: Station[]
  orders?: Order[]
  polylines?: MapPolyline[]
  onPolylineHover?: (truckId: string | null) => void
  onPolylineClick?: (truckId: string, polyline: MapPolyline) => void
  hoveredPolylineId?: string | null
}

const CELL_SIZE = 5
const GRID_WIDTH = 70
const GRID_HEIGHT = 50

export default function DynamicMap({
  points = [],
  trucks = [],
  stations = [],
  orders = [],
  polylines = [],
  onPolylineHover,
  onPolylineClick,
  hoveredPolylineId,
}: DynamicMapProps) {
  const { theme } = useTheme()
  const isDark =
    theme === 'dark' ||
    (theme === 'system' &&
      window.matchMedia &&
      window.matchMedia('(prefers-color-scheme: dark)').matches)
  const [zoom, setZoom] = useState(1)
  const [center, setCenter] = useState<[number, number]>([0, 0])

  const gridColor = isDark ? '#444' : '#000'
  const pointFill = isDark ? '#90cdf4' : '#3355FF'
  const pointStroke = isDark ? '#222' : '#FFF'

  const customProjection = geoIdentity() as unknown as ProjectionFunction

  const verticalLines = useMemo(() => {
    const lines = []
    for (let y = 0; y <= GRID_WIDTH; y++) {
      lines.push(
        <Line
          key={`v-${y}`}
          coordinates={[
            [y * CELL_SIZE, 0],
            [y * CELL_SIZE, GRID_HEIGHT * CELL_SIZE],
          ]}
          stroke={gridColor}
          strokeWidth={0.1}
        />,
      )
    }
    return lines
  }, [gridColor])

  const horizontalLines = useMemo(() => {
    const lines = []
    for (let x = 0; x <= GRID_HEIGHT; x++) {
      lines.push(
        <Line
          key={`h-${x}`}
          coordinates={[
            [0, x * CELL_SIZE],
            [GRID_WIDTH * CELL_SIZE, x * CELL_SIZE],
          ]}
          stroke={gridColor}
          strokeWidth={0.1}
        />,
      )
    }
    return lines
  }, [gridColor])

  const transformedPoints = useMemo(
    () =>
      points.map(([x, y]) => [x * CELL_SIZE, (GRID_HEIGHT - y) * CELL_SIZE]),
    [points],
  )

  const transformLocation = useMemo(
    () => (location: Location) =>
      [location.x * CELL_SIZE, (GRID_HEIGHT - location.y) * CELL_SIZE] as [
        number,
        number,
      ],
    [],
  )

  const transformRawPoint = useMemo(
    () =>
      (point: [number, number]): [number, number] =>
        [point[0] * CELL_SIZE, (GRID_HEIGHT - point[1]) * CELL_SIZE] as [
          number,
          number,
        ],
    [],
  )

  const handlePolylineHover = (polylineId: string | null) => {
    onPolylineHover?.(polylineId)
  }

  const handlePolylineClick = useCallback(
    (polylineId: string, polyline: MapPolyline) => {
      onPolylineClick?.(polylineId, polyline)
    },
    [onPolylineClick],
  )

  const anyPolylineHovered = !!hoveredPolylineId

  return (
    <div className="p-1 h-full flex flex-col justify-center overflow-hidden box-border">
      <div className="w-full h-full max-h-[calc(100vh-2rem)] flex justify-center items-center">
        <div
          className={`w-full h-full aspect-[${GRID_WIDTH}/${GRID_HEIGHT}]`}
          style={{
            maxWidth: `calc((100vh - 2rem) * ${GRID_WIDTH / GRID_HEIGHT})`,
            maxHeight: `calc((100vw) * ${GRID_HEIGHT / GRID_WIDTH})`,
          }}
        >
          <ComposableMap
            projection={customProjection}
            projectionConfig={{ scale: 1 }}
            viewBox={`0 0 ${GRID_WIDTH * CELL_SIZE} ${GRID_HEIGHT * CELL_SIZE}`}
            className={`w-full h-full cursor-grab${anyPolylineHovered ? ` ${styles.mapHasHover}` : ''}`}
          >
            <ZoomableGroup
              zoom={zoom}
              center={center}
              onMoveEnd={({ zoom, coordinates }) => {
                setZoom(zoom)
                setCenter(coordinates)
              }}
              translateExtent={[
                [0, 0],
                [GRID_WIDTH * CELL_SIZE, GRID_HEIGHT * CELL_SIZE],
              ]}
            >
              {verticalLines}
              {horizontalLines}

              {transformedPoints.map(([x, y]) => (
                <Marker key={`point-${x}-${y}`} coordinates={[x, y]}>
                  <circle
                    r={3}
                    fill={pointFill}
                    stroke={pointStroke}
                    strokeWidth={0.1}
                  />
                </Marker>
              ))}

              {trucks.map((truck) => {
                const [cx, cy] = transformLocation(truck.location)
                return (
                  <TruckMarker key={truck.id} truck={truck} cx={cx} cy={cy} />
                )
              })}

              {stations.map((station) => {
                const [cx, cy] = transformLocation(station.location)
                return (
                  <StationMarker
                    key={station.id}
                    station={station}
                    cx={cx}
                    cy={cy}
                  />
                )
              })}

              {orders.map((order) => {
                const [cx, cy] = transformLocation(order.location)
                return (
                  <OrderMarker key={order.id} order={order} cx={cx} cy={cy} />
                )
              })}

              {/* Render polylines after markers so they appear above all icons */}
              {polylines.map((polyline) => (
                <PolylineItem
                  key={polyline.id}
                  polyline={polyline}
                  transformRawPoint={transformRawPoint}
                  isHovered={polyline.id === hoveredPolylineId}
                  onPolylineHover={handlePolylineHover}
                  onPolylineClick={handlePolylineClick}
                  hoveredPolylineId={hoveredPolylineId}
                />
              ))}
            </ZoomableGroup>
          </ComposableMap>
        </div>
      </div>
    </div>
  )
}

export const formatTooltipText = (obj: object) => {
  return Object.entries(obj)
    .map(([key, value]) => `${key}: ${value}`)
    .join('\n')
}
